package com.vku.lanmonitor.server.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vku.lanmonitor.server.model.ClientSession;
import com.vku.lanmonitor.server.service.QuizService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// import org.jcodec.api.JCodecException;
// import org.jcodec.common.io.NIOUtils;
// import java.awt.image.BufferedImage;
// import javax.imageio.ImageIO;

@Slf4j
@Service
@RequiredArgsConstructor
public class TcpServerService implements CommandLineRunner {

    private static final java.io.PrintStream OUT = new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8);

    private static final String ADMIN_USER = "admin";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String STORAGE_DIR = "storage";
    private static final String RULES_FILE = STORAGE_DIR + "/exam_rules.json";

    private final TcpBroadcastService broadcastService;
    private final QuizService quizService;
    private final ObjectMapper mapper = new ObjectMapper();

    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final Map<String, ClientSession> activeClients = new ConcurrentHashMap<>();
    private final Map<String, Boolean> captureRequests = new ConcurrentHashMap<>();

    public void requestCapture(String clientId) {
        captureRequests.put(clientId, true);
    }

    @Override
    public void run(String... args) {
        new Thread(this::startServer).start();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(9999);
            isRunning = true;
            OUT.println("Server connected (Port 9999)");

            Files.createDirectories(Paths.get("images"));
            Files.createDirectories(Paths.get(STORAGE_DIR));

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (Exception e) {
            log.error("Lỗi Server: {}", e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String connectionId;
        private String role = "UNKNOWN";
        private String clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.connectionId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            this.clientId = this.connectionId;
        }

        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = in.readLine()) != null) {
                    handleLine(line, out);
                }
            } catch (Exception e) {
                log.debug("Kết nối [{}] đóng: {}", connectionId, e.getMessage());
            } finally {
                boolean wasRegistered = !"UNKNOWN".equals(role);
                boolean wasClient = "CLIENT".equals(role);

                if (wasRegistered) {
                    ClientSession removed = activeClients.remove(clientId);
                    captureRequests.remove(clientId);
                    broadcastService.unregister(connectionId);
                    broadcastService.unregister(clientId);

                    if (removed != null || wasClient) {
                        String timeStr = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy")
                                .format(new java.util.Date());
                        String name = (removed != null) ? removed.getPcName() : clientId;
                        String logLine = timeStr + " Máy: " + name + ", IP: " + connectionId + " đã rời đi";
                        broadcastService.broadcastToAdmins("SERVER_LOG:" + logLine);
                    }
                }

                if (wasClient) {
                    broadcastClientList();
                }
            }
        }

        private void handleLine(String line, PrintWriter out) {
            try {
                if (line.startsWith("STUDENT_LOGIN:")) {
                    handleStudentLogin(payload(line, "STUDENT_LOGIN:"), out);
                } else if (line.startsWith("ADMIN_LOGIN:")) {
                    handleAdminLogin(payload(line, "ADMIN_LOGIN:"), out);
                } else if (line.startsWith("CONNECT:")) {
                    handleConnect(payload(line, "CONNECT:"), out);
                } else if (line.startsWith("REQ_QUIZ_LIST")) {
                    out.println("RES_QUIZ_LIST:" + mapper.writeValueAsString(quizService.loadAllQuizzes()));
                } else if (line.startsWith("REQ_QUIZ_DETAIL:")) {
                    Object quiz = quizService.loadQuizById(payload(line, "REQ_QUIZ_DETAIL:"));
                    out.println("RES_QUIZ_DETAIL:" + (quiz == null ? "null" : mapper.writeValueAsString(quiz)));
                } else if (line.startsWith("SUBMIT_QUIZ:")) {
                    handleSubmitQuiz(payload(line, "SUBMIT_QUIZ:"), out);
                } else if (line.startsWith("REQ_CLIENT_LIST")) {
                    List<Map<String, String>> details = new ArrayList<>();
                    for (ClientSession session : activeClients.values()) {
                        Map<String, String> c = new HashMap<>();
                        c.put("id", session.getId());
                        c.put("ip", session.getIpAddress());
                        c.put("pcName", session.getPcName());
                        details.add(c);
                    }
                    out.println("RES_CLIENT_LIST:" + mapper.writeValueAsString(details));
                } else if (line.startsWith("REQ_QUIZ_ALL")) {
                    out.println("RES_QUIZ_ALL:" + mapper.writeValueAsString(quizService.loadAllQuizzes()));
                } else if (line.startsWith("SAVE_QUIZ:")) {
                    if (!"ADMIN".equals(role)) {
                        out.println("ERROR:admin_required");
                        return;
                    }
                    handleSaveQuiz(payload(line, "SAVE_QUIZ:"), out);
                } else if (line.startsWith("DELETE_QUIZ:")) {
                    if (!"ADMIN".equals(role)) {
                        out.println("ERROR:admin_required");
                        return;
                    }
                    handleDeleteQuiz(payload(line, "DELETE_QUIZ:"), out);
                } else if (line.startsWith("CAPTURE:")) {
                    handleCapture(payload(line, "CAPTURE:"), out);
                } else if (line.startsWith("SEND_ALERT:")) {
                    handleSendAlert(payload(line, "SEND_ALERT:"));
                } else if (line.startsWith("SAVE_FRAME:")) {
                    requestCapture(resolveClientId(payload(line, "SAVE_FRAME:")));
                } else if (line.startsWith("REQ_WHITELIST")) {
                    out.println("RES_WHITELIST:" + String.join(",", loadWhitelist()));
                } else if (line.startsWith("UPDATE_WHITELIST_CONFIG:")) {
                    if (!"ADMIN".equals(role)) {
                        out.println("ERROR:admin_required");
                        return;
                    }
                    handleUpdateWhitelist(payload(line, "UPDATE_WHITELIST_CONFIG:"), out);
                } else if (line.startsWith("SCREEN:")) {
                    handleScreen(payload(line, "SCREEN:"));
                } else if (line.startsWith("SUSPICIOUS:")) {
                    handleSuspicious(payload(line, "SUSPICIOUS:"));
                } else if (line.startsWith("BROADCAST_MSG:")) {
                    if (!"ADMIN".equals(role)) {
                        out.println("ERROR:admin_required");
                        return;
                    }
                    handleBroadcastMsg(payload(line, "BROADCAST_MSG:"), out);
                } else if (line.startsWith("ACK_MSG:")) {
                    handleAckMsg(payload(line, "ACK_MSG:"));
                } else {
                    log.debug("Lệnh không nhận diện từ [{}]: {}", connectionId, line);
                }
            } catch (Exception e) {
                log.error("Lỗi xử lý lệnh từ [{}]: {}", connectionId, e.getMessage());
                out.println("ERROR:" + e.getMessage());
            }
        }

        private void handleStudentLogin(String payload, PrintWriter out) {
            String[] parts = payload.split(":", 2);
            if (parts.length < 2 || parts[0].isBlank() || !DEFAULT_PASSWORD.equals(parts[1])) {
                out.println("LOGIN_FAIL");
                return;
            }
            role = "STUDENT";
            clientId = parts[0].trim();
            broadcastService.registerStudent(connectionId, out);
            out.println("LOGIN_OK");
        }

        private void handleAdminLogin(String payload, PrintWriter out) {
            String[] parts = payload.split(":", 2);
            if (parts.length < 2 || !ADMIN_USER.equals(parts[0]) || !DEFAULT_PASSWORD.equals(parts[1])) {
                out.println("ADMIN_FAIL");
                return;
            }
            role = "ADMIN";
            clientId = connectionId;
            broadcastService.registerAdmin(connectionId, out);
            out.println("ADMIN_OK");
        }

        private void handleConnect(String pcName, PrintWriter out) throws Exception {
            role = "CLIENT";
            String displayName = (pcName == null || pcName.isBlank()) ? "Chưa nhập tên" : pcName;
            ClientSession session = new ClientSession(connectionId, socket.getInetAddress().getHostAddress(),
                    displayName);
            clientId = connectionId;
            activeClients.put(clientId, session);
            broadcastService.registerClient(clientId, out);
            out.println("UPDATE_WHITELIST:" + String.join(",", loadWhitelist()));
            broadcastClientList();

            String timeStr = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy").format(new java.util.Date());
            String logLine = timeStr + " Máy: " + displayName + ", IP: " + connectionId + " đã tham gia";
            broadcastService.broadcastToAdmins("SERVER_LOG:" + logLine);
        }

        @SuppressWarnings("unchecked")
        private void handleSubmitQuiz(String json, PrintWriter out) {
            try {
                Map<String, Object> resultData = mapper.readValue(json, Map.class);
                quizService.saveQuizResult(resultData);
                out.println("SUBMIT_OK");
            } catch (Exception e) {
                log.error("Lỗi nộp bài: ", e);
                out.println("SUBMIT_FAIL:" + e.getMessage());
            }
        }

        @SuppressWarnings("unchecked")
        private void handleSaveQuiz(String json, PrintWriter out) {
            try {
                String trimmed = json.trim();
                if (trimmed.startsWith("[")) {
                    quizService.saveAllQuizzes(mapper.readValue(trimmed, List.class));
                } else {
                    Object quiz = mapper.readValue(trimmed, Object.class);
                    List<Object> all = new ArrayList<>(quizService.loadAllQuizzes());
                    String quizId = quiz instanceof Map<?, ?> map ? String.valueOf(map.get("id")) : null;
                    boolean replaced = false;
                    if (quizId != null && !"null".equals(quizId)) {
                        for (int i = 0; i < all.size(); i++) {
                            Object item = all.get(i);
                            if (item instanceof Map<?, ?> map && quizId.equals(String.valueOf(map.get("id")))) {
                                all.set(i, quiz);
                                replaced = true;
                                break;
                            }
                        }
                    }
                    if (!replaced) {
                        all.add(quiz);
                    }
                    quizService.saveAllQuizzes(all);
                }
                out.println("SAVE_QUIZ_OK");
            } catch (Exception e) {
                log.error("Lỗi lưu đề thi: ", e);
                out.println("SAVE_QUIZ_FAIL:" + e.getMessage());
            }
        }

        private void handleDeleteQuiz(String quizId, PrintWriter out) {
            try {
                List<Object> all = new ArrayList<>(quizService.loadAllQuizzes());
                boolean removed = all.removeIf(item -> item instanceof Map<?, ?> map
                        && quizId.equals(String.valueOf(map.get("id"))));
                if (removed) {
                    quizService.saveAllQuizzes(all);
                    out.println("DELETE_QUIZ_OK");
                } else {
                    out.println("DELETE_QUIZ_FAIL:not_found");
                }
            } catch (Exception e) {
                log.error("Lỗi xóa đề thi: ", e);
                out.println("DELETE_QUIZ_FAIL:" + e.getMessage());
            }
        }

        private void handleCapture(String target, PrintWriter out) {
            String resolved = resolveClientId(target);
            if (resolved == null) {
                out.println("CAPTURE_FAIL:not_found");
                return;
            }
            broadcastService.sendToClient(resolved, "CAPTURE_SCREEN");
        }

        private void handleSendAlert(String payload) {
            String[] parts = payload.split(":", 2);
            if (parts.length == 0 || parts[0].isBlank()) {
                return;
            }
            String resolved = resolveClientId(parts[0]);
            if (resolved != null) {
                String message = parts.length > 1 ? parts[1] : "";
                broadcastService.sendToClient(resolved, "ALERT:" + message);
            }
        }

        private void handleUpdateWhitelist(String keywords, PrintWriter out) {
            try {
                List<String> newWhitelist = Arrays.asList(keywords.split(","));
                Files.createDirectories(Paths.get(STORAGE_DIR));
                mapper.writeValue(new File(RULES_FILE), newWhitelist);
                broadcastService.broadcastToClients("UPDATE_WHITELIST:" + String.join(",", newWhitelist));
                out.println("UPDATE_WHITELIST_OK");
            } catch (Exception e) {
                log.error("Lỗi cập nhật whitelist: ", e);
                out.println("UPDATE_WHITELIST_FAIL:" + e.getMessage());
            }
        }

        private void handleScreen(String base64Image) {
            broadcastService.broadcastToAdmins("SCREEN_STREAM:" + connectionId + ":" + base64Image);
            if (clientId != null && captureRequests.getOrDefault(clientId, false)) {
                captureRequests.put(clientId, false);
                try {
                    String safeId = sanitizeFileName(clientId);
                    String fName = "cheat_" + safeId + "_" + System.currentTimeMillis() + ".jpg";
                    java.nio.file.Path filePath = safeResolve(Paths.get("images"), fName);
                    if (filePath != null) {
                        Files.write(filePath, Base64.getDecoder().decode(base64Image));
                    } else {
                        log.warn("Rejected unsafe file path for client: {}", clientId);
                    }
                } catch (Exception e) {
                    log.error("Lỗi lưu ảnh: ", e);
                }
            }
        }

        private void handleSuspicious(String payload) {
            ClientSession session = activeClients.get(clientId);
            String pcName = session != null ? session.getPcName() : clientId;
            broadcastService.broadcastToAdmins("SUSPICIOUS:" + clientId + "|" + pcName + "|" + payload);

            String timeStr = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy").format(new java.util.Date());
            String logLine = timeStr + " Máy: " + pcName + ", IP: " + clientId + " Đang chuyển tab";
            broadcastService.broadcastToAdmins("SERVER_LOG:" + logLine);
        }

        private void handleBroadcastMsg(String payload, PrintWriter out) {
            String[] parts = payload.split(":");
            if (parts.length < 2) {
                out.println("ERROR:invalid_broadcast_format");
                return;
            }
            String encodedContent = parts[parts.length - 1];
            String target = String.join(":", java.util.Arrays.copyOf(parts, parts.length - 1));
            if ("ALL".equals(target)) {
                for (ClientSession cs : activeClients.values()) {
                    broadcastService.sendToClient(cs.getId(), "RECV_MSG:" + encodedContent);
                }
            } else {
                ClientSession cs = activeClients.get(target);
                if (cs != null) {
                    broadcastService.sendToClient(cs.getId(), "RECV_MSG:" + encodedContent);
                } else {
                    out.println("ERROR:client_not_found:" + target);
                }
            }
        }

        private void handleAckMsg(String clientId) {
        }
    }

    private void broadcastClientList() {
        try {
            broadcastService.broadcastToAdmins("BROADCAST_CLIENTS:" + mapper.writeValueAsString(clientNames()));
        } catch (Exception e) {
            log.error("Lỗi broadcast danh sách client: ", e);
        }
    }

    private List<String> clientNames() {
        List<String> names = new ArrayList<>();
        for (ClientSession session : activeClients.values()) {
            names.add(session.getPcName());
        }
        return names;
    }

    private String resolveClientId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String trimmed = token.trim();
        if (activeClients.containsKey(trimmed)) {
            return trimmed;
        }
        for (ClientSession session : activeClients.values()) {
            if (trimmed.equals(session.getPcName())) {
                return session.getId();
            }
        }
        return null;
    }

    private List<String> loadWhitelist() {
        try {
            File file = new File(RULES_FILE);
            if (file.exists()) {
                return Arrays.asList(mapper.readValue(file, String[].class));
            }
        } catch (Exception e) {
            log.error("Lỗi đọc whitelist: ", e);
        }
        return Arrays.asList("Bài Thi Trắc Nghiệm", "Word", "Calculator", "Excel");
    }

    private static String payload(String line, String prefix) {
        return line.length() > prefix.length() ? line.substring(prefix.length()) : "";
    }

    /**
     * Sanitize filename to prevent path traversal attacks.
     * Removes path separators, ".." sequences, and other dangerous characters.
     */
    private static String sanitizeFileName(String input) {
        if (input == null) {
            return "unknown";
        }
        // Remove path traversal sequences and separators
        String sanitized = input
                .replaceAll("\\.\\.", "") // Remove ".." sequences
                .replaceAll("[\\\\/:*?\"<>|]", "_"); // Replace dangerous characters
        // Remove any remaining path separators
        sanitized = sanitized.replaceAll("[/\\\\]", "_");
        // Ensure we don't have empty or overly long filenames
        if (sanitized.isEmpty() || sanitized.length() > 100) {
            sanitized = "unknown";
        }
        return sanitized;
    }

    /**
     * Safely resolve a path within a base directory, preventing path traversal.
     */
    private static java.nio.file.Path safeResolve(java.nio.file.Path baseDir, String... paths) {
        try {
            java.nio.file.Path resolved = baseDir;
            for (String path : paths) {
                if (path == null)
                    continue;
                // Sanitize each component
                String sanitized = sanitizeFileName(path);
                resolved = resolved.resolve(sanitized);
            }
            // Ensure the resolved path is within the base directory
            java.nio.file.Path normalizedBase = baseDir.toAbsolutePath().normalize();
            java.nio.file.Path normalizedResolved = resolved.toAbsolutePath().normalize();
            if (!normalizedResolved.startsWith(normalizedBase)) {
                log.warn("Path traversal attempt detected: {} -> {}", baseDir, resolved);
                return null;
            }
            return resolved;
        } catch (Exception e) {
            log.error("Error resolving path: {}", e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (Exception ignored) {
        }
    }
}
