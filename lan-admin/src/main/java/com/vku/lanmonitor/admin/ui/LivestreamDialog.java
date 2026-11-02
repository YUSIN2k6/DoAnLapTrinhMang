package com.vku.lanmonitor.admin.ui;

import com.vku.lanmonitor.admin.net.AdminMessageDispatcher;
import com.vku.lanmonitor.admin.net.AdminTcpClient;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.Consumer;

public class LivestreamDialog extends JDialog {
    private final AdminTcpClient tcpClient;
    private final AdminMessageDispatcher dispatcher;
    private final String clientId;
    private final JLabel imageLabel;
    private final JLabel statusLabel;
    private javax.swing.Timer captureTimer;
    private Consumer<String> messageHandler;

    public LivestreamDialog(JFrame parent, AdminTcpClient tcpClient,
                            AdminMessageDispatcher dispatcher,
                            String clientId, String pcName) {
        super(parent, "Live - " + pcName, false);
        this.tcpClient = tcpClient;
        this.dispatcher = dispatcher;
        this.clientId = clientId;

        setSize(1100, 750);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(5, 5));

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(new Color(30, 30, 40));
        north.setPreferredSize(new Dimension(0, 50));

        JLabel title = new JLabel("  LIVE: " + pcName);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        north.add(title, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPanel.setOpaque(false);
        JButton btnCapture = new JButton("Chụp ảnh minh chứng");
        btnCapture.setBackground(new Color(200, 50, 50));
        btnCapture.setForeground(Color.WHITE);
        btnCapture.setOpaque(true);
        btnCapture.setBorderPainted(false);
        JButton btnClose = new JButton("Đóng");
        btnPanel.add(btnCapture);
        btnPanel.add(btnClose);
        north.add(btnPanel, BorderLayout.EAST);
        add(north, BorderLayout.NORTH);

        imageLabel = new JLabel("Đang kết nối...", SwingConstants.CENTER);
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.BLACK);
        imageLabel.setForeground(Color.WHITE);
        imageLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        add(imageLabel, BorderLayout.CENTER);

        statusLabel = new JLabel("Bắt đầu livestream...");
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(255, 255, 200));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.SOUTH);

        messageHandler = this::handleServerMessage;
        dispatcher.addHandler(messageHandler);

        captureTimer = new javax.swing.Timer(500, e -> {
            System.out.println("[LIVE] Timer tick, sending CAPTURE:" + clientId);
            tcpClient.sendCommand("CAPTURE:" + clientId);
        });
        captureTimer.start();

        tcpClient.sendCommand("CAPTURE:" + clientId);
        System.out.println("[LIVE] Sent CAPTURE:" + clientId);

        btnCapture.addActionListener(e -> {
            tcpClient.sendCommand("SAVE_FRAME:" + clientId);
            statusLabel.setText("Đã gửi lệnh lưu ảnh");
        });
        btnClose.addActionListener(e -> dispose());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });

        setVisible(true);
    }

    private void handleServerMessage(String message) {
        System.out.println("[LIVE] Received: " + message.substring(0, Math.min(80, message.length())));
        if (message.startsWith("SCREEN_STREAM:")) {
            String rest = message.substring("SCREEN_STREAM:".length());
            int firstColon = rest.indexOf(':');
            if (firstColon < 0) return;
            int secondColon = rest.indexOf(':', firstColon + 1);
            if (secondColon < 0) return;
            String senderId = rest.substring(0, secondColon);
            if (!senderId.equals(clientId)) {
                System.out.println("[LIVE] Ignore stream from " + senderId + " (expect " + clientId + ")");
                return;
            }
            String base64 = rest.substring(secondColon + 1);
            System.out.println("[LIVE] Matched! base64 len=" + base64.length());
            SwingUtilities.invokeLater(() -> updateImage(base64));
        }
    }

    private void updateImage(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return;

            int lw = imageLabel.getWidth();
            int lh = imageLabel.getHeight();
            if (lw <= 0 || lh <= 0) {
                imageLabel.setIcon(new ImageIcon(img));
                System.out.println("[LIVE] Image updated successfully");
                return;
            }
            double scale = Math.min((double) lw / img.getWidth(),
                                    (double) lh / img.getHeight());
            int nw = (int) (img.getWidth() * scale);
            int nh = (int) (img.getHeight() * scale);
            Image scaled = img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
            imageLabel.setText("");
            statusLabel.setText("Cập nhật: " + new java.text.SimpleDateFormat("HH:mm:ss")
                    .format(new java.util.Date()));
            System.out.println("[LIVE] Image updated successfully");
        } catch (Exception e) {
            statusLabel.setText("Lỗi decode ảnh: " + e.getMessage());
        }
    }

    private void cleanup() {
        if (captureTimer != null) captureTimer.stop();
        if (messageHandler != null) dispatcher.removeHandler(messageHandler);
    }

    @Override
    public void dispose() {
        cleanup();
        super.dispose();
    }
}
