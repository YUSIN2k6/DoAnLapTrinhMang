package com.vku.lanmonitor.client.service;

import com.vku.lanmonitor.client.util.ActiveWindowUtil;
import com.vku.lanmonitor.client.util.ScreenCaptureUtil;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SocketClientService {

    private final String serverIp;
    private final int serverPort;
    private final String studentId;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean isConnected = false;
    private volatile boolean shouldStop = false;
    private String lastAlertTab = "";
    private ScheduledExecutorService monitorScheduler;

    private List<String> whitelist = new ArrayList<>();

    public SocketClientService(String serverIp, int serverPort, String studentId) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.studentId = studentId;
    }

    public void start() {
        startBehaviorMonitor();

        while (!shouldStop) {
            try {
                socket = new Socket(serverIp, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                isConnected = true;

                // Báo danh với Server ngay khi vừa kết nối
                out.println("CONNECT:" + studentId);
                out.flush();
                System.out.println("Client connected (" + serverIp + ":" + serverPort + ")");

                String line;
                while (isConnected && !shouldStop && (line = in.readLine()) != null) {
                    handleCommand(line);
                }
            } catch (Exception e) {
            } finally {
                isConnected = false;
                cleanupConnection();
            }

            if (!shouldStop) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void stop() {
        cleanup();
    }

    private void cleanupConnection() {
        try {
            if (in != null)
                in.close();
        } catch (Exception ignored) {
        }
        try {
            if (out != null)
                out.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null)
                socket.close();
        } catch (Exception ignored) {
        }
    }

    private void startBehaviorMonitor() {
        monitorScheduler = Executors.newSingleThreadScheduledExecutor();
        monitorScheduler.scheduleAtFixedRate(() -> {
            try {
                String activeTitle = ActiveWindowUtil.getActiveWindowTitle();
                if (activeTitle.isEmpty() || activeTitle.equals(lastAlertTab)) {
                    return;
                }

                if (whitelist.isEmpty())
                    return;

                if (containsKeyword(activeTitle, whitelist)) {
                    lastAlertTab = activeTitle;
                    return;
                }

                // Gửi mã cảnh báo vi phạm về Server
                if (out != null && isConnected) {
                    out.println("SUSPICIOUS:RED|" + activeTitle);
                }

                lastAlertTab = activeTitle;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS); // Mỗi 1 giây quét 1 lần
    }

    private boolean containsKeyword(String title, List<String> keywords) {
        String lowerTitle = title.toLowerCase();
        for (String kw : keywords) {
            if (kw.trim().isEmpty())
                continue;
            if (lowerTitle.contains(kw.toLowerCase().trim()))
                return true;
        }
        return false;
    }

    private void handleCommand(String command) {
        if (command.startsWith("ALERT:")) {
            String message = command.substring(6);
            new Thread(() -> {
                JOptionPane.showMessageDialog(null, message, "Thông báo từ Giám thị",
                        JOptionPane.WARNING_MESSAGE);
            }).start();
        } else if ("CAPTURE_SCREEN".equals(command)) {
            byte[] imgBytes = ScreenCaptureUtil.captureCurrentScreen();
            if (imgBytes != null && imgBytes.length > 0) {
                String base64 = Base64.getEncoder().encodeToString(imgBytes);
                out.println("SCREEN:" + base64);
            }
        } else if (command.startsWith("UPDATE_WHITELIST:")) {
            String keywordsStr = command.substring(17);
            whitelist = new ArrayList<>(Arrays.asList(keywordsStr.split(",")));
        } else if (command.startsWith("RECV_MSG:")) {
            String encoded = command.substring("RECV_MSG:".length());
            String content = new String(java.util.Base64.getDecoder().decode(encoded),
                    java.nio.charset.StandardCharsets.UTF_8);
            SwingUtilities.invokeLater(() -> {
                JOptionPane pane = new JOptionPane(content, JOptionPane.WARNING_MESSAGE);
                JDialog msgDialog = pane.createDialog("[!] Thông báo từ Giám thị");
                msgDialog.setAlwaysOnTop(true);
                msgDialog.setVisible(true);
            });
            out.println("ACK_MSG:" + studentId);
        }
    }

    private void cleanup() {
        shouldStop = true;
        if (monitorScheduler != null)
            monitorScheduler.shutdownNow();
        cleanupConnection();
    }
}
