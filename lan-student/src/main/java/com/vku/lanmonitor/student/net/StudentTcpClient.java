package com.vku.lanmonitor.student.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class StudentTcpClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> messageListener;
    private Thread readerThread;
    private volatile boolean isConnected = false;
    private volatile boolean shouldStop = false;

    public StudentTcpClient() {
    }

    public void connect(String host, int port) throws Exception {
        if (isConnected) {
            disconnect();
        }
        
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        isConnected = true;
        shouldStop = false;

        readerThread = new Thread(this::readMessages);
        readerThread.setDaemon(true);
        readerThread.start();

        notifyListener("CONNECTED");
    }

    public void disconnect() {
        shouldStop = true;
        isConnected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
        if (readerThread != null && readerThread.isAlive()) {
            try {
                readerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        notifyListener("DISCONNECTED");
    }

    public void sendCommand(String cmd) {
        if (!isConnected || out == null) {
            notifyListener("ERROR:Không kết nối tới server");
            return;
        }
        out.println(cmd);
    }

    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void readMessages() {
        try {
            String line;
            while (!shouldStop && (line = in.readLine()) != null) {
                notifyListener(line);
            }
        } catch (Exception e) {
            if (!shouldStop) {
                notifyListener("ERROR:Mất kết nối server: " + e.getMessage());
            }
        } finally {
            isConnected = false;
        }
    }

    private void notifyListener(String message) {
        if (messageListener != null) {
            messageListener.accept(message);
        }
    }
}
