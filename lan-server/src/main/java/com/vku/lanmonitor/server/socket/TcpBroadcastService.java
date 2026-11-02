package com.vku.lanmonitor.server.socket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TcpBroadcastService {

    private final Map<String, PrintWriter> studentConnections = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> adminConnections = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> clientConnections = new ConcurrentHashMap<>();

    public void registerStudent(String id, PrintWriter out) {
        studentConnections.put(id, out);
        log.info("Đăng ký student [{}]", id);
    }

    public void registerAdmin(String id, PrintWriter out) {
        adminConnections.put(id, out);
        log.info("Đăng ký admin [{}]", id);
    }

    public void registerClient(String id, PrintWriter out) {
        clientConnections.put(id, out);
        log.info("Đăng ký client [{}]", id);
    }

    public void unregister(String id) {
        studentConnections.remove(id);
        adminConnections.remove(id);
        clientConnections.remove(id);
        log.info("Hủy đăng ký [{}]", id);
    }

    public void broadcastToStudents(String msg) {
        studentConnections.values().forEach(out -> out.println(msg));
    }

    public void broadcastToAdmins(String msg) {
        adminConnections.values().forEach(out -> out.println(msg));
    }

    public void broadcastToClients(String msg) {
        clientConnections.values().forEach(out -> out.println(msg));
    }

    public void sendToClient(String clientId, String msg) {
        PrintWriter out = clientConnections.get(clientId);
        if (out != null) {
            out.println(msg);
        }
    }
}
