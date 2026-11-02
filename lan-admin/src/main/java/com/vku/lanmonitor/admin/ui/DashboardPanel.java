package com.vku.lanmonitor.admin.ui;

import com.vku.lanmonitor.admin.net.AdminMessageDispatcher;
import com.vku.lanmonitor.admin.net.AdminTcpClient;
import com.vku.lanmonitor.admin.ui.theme.LanMonitorTheme;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Panel:
 * - Trái: Danh sách máy trạm kết nối
 * - Phải: Danh sách cảnh báo hành vi khả nghi (khi học sinh đổi tab / vi phạm)
 */
public class DashboardPanel extends JPanel {

    private final AdminTcpClient tcpClient;
    private final DefaultListModel<String> clientListModel;
    private final JList<String> clientList;
    private final JLabel lblCount;
    private final JLabel lblStatus;
    private javax.swing.Timer refreshTimer;
    private List<Map<String, Object>> currentClients;

    // Khu vực cảnh báo hành vi khả nghi (Bên phải)
    private final JPanel alertListContainer;
    private final JScrollPane alertScrollPane;

    public DashboardPanel(AdminTcpClient tcpClient, AdminMessageDispatcher dispatcher) {
        this.tcpClient = tcpClient;
        setLayout(new BorderLayout(8, 8));
        setBackground(LanMonitorTheme.LIGHT_BG);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // ========================== CỘT TRÁI: DANH SÁCH MÁY TRẠM
        // ==========================
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(LanMonitorTheme.LIGHT_BG);
        leftPanel.setPreferredSize(new Dimension(460, 0));

        // Header cột trái
        JPanel leftHeader = new JPanel(new BorderLayout(5, 5));
        leftHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("Danh sách máy trạm");
        lblTitle.setFont(LanMonitorTheme.FONT_TITLE);
        lblCount = new JLabel("0 máy");
        lblCount.setFont(LanMonitorTheme.FONT_BOLD);
        lblCount.setForeground(LanMonitorTheme.BLUE_BTN);

        JPanel leftTitleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftTitleBox.setOpaque(false);
        leftTitleBox.add(lblTitle);
        leftTitleBox.add(lblCount);
        leftHeader.add(leftTitleBox, BorderLayout.WEST);

        // Thanh nút thao tác
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        toolbar.setOpaque(false);
        JButton btnRefresh = LanMonitorTheme.createSecondaryButton("Làm mới");
        JButton btnLive = LanMonitorTheme.createSecondaryButton("Xem màn hình");
        JButton btnSaveFrame = LanMonitorTheme.createSecondaryButton("Chụp ảnh");
        JButton btnBroadcast = LanMonitorTheme.createPrimaryButton("Gửi thông báo", LanMonitorTheme.ORANGE_BTN);
        toolbar.add(btnRefresh);
        toolbar.add(btnLive);
        toolbar.add(btnSaveFrame);
        toolbar.add(btnBroadcast);
        leftHeader.add(toolbar, BorderLayout.SOUTH);
        leftPanel.add(leftHeader, BorderLayout.NORTH);

        // Danh sách JList
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setFont(LanMonitorTheme.FONT_BASE);
        clientList.setBackground(Color.WHITE);
        clientList.setSelectionBackground(LanMonitorTheme.PURPLE_HEADER);
        clientList.setSelectionForeground(Color.WHITE);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(BorderFactory.createLineBorder(LanMonitorTheme.LIGHT_CONTENT));
        leftPanel.add(clientScroll, BorderLayout.CENTER);

        // Footer cột trái
        lblStatus = new JLabel("Chưa có kết nối");
        lblStatus.setForeground(LanMonitorTheme.TEXT_MUTED);
        lblStatus.setFont(LanMonitorTheme.FONT_SMALL);
        lblStatus.setBorder(new EmptyBorder(4, 4, 4, 4));
        leftPanel.add(lblStatus, BorderLayout.SOUTH);

        // ========================== CỘT PHẢI: HÀNH VI KHẢ NGHI
        // ==========================
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(LanMonitorTheme.LIGHT_BG);

        JPanel rightHeader = new JPanel(new BorderLayout(5, 5));
        rightHeader.setOpaque(false);
        rightHeader.setBorder(new EmptyBorder(0, 0, 4, 0));

        JLabel alertTitle = new JLabel("Cảnh báo hành vi khả nghi (Chuyển tab / Vi phạm)");
        alertTitle.setFont(LanMonitorTheme.FONT_TITLE);
        alertTitle.setForeground(new Color(0xB9, 0x1C, 0x1C)); // Đỏ cảnh báo

        JButton btnClearAlerts = LanMonitorTheme.createSecondaryButton("Xóa lịch sử");
        btnClearAlerts.addActionListener(e -> clearAlerts());

        rightHeader.add(alertTitle, BorderLayout.WEST);
        rightHeader.add(btnClearAlerts, BorderLayout.EAST);
        rightPanel.add(rightHeader, BorderLayout.NORTH);

        // Container danh sách cảnh báo theo dạng xếp dọc
        alertListContainer = new JPanel();
        alertListContainer.setLayout(new BoxLayout(alertListContainer, BoxLayout.Y_AXIS));
        alertListContainer.setBackground(new Color(0x0F, 0x17, 0x2A)); // Dark Navy

        alertScrollPane = new JScrollPane(alertListContainer);
        alertScrollPane.setBorder(BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55)));
        alertScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rightPanel.add(alertScrollPane, BorderLayout.CENTER);

        // ========================== SPLIT PANE TRÁI - PHẢI ==========================
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(460);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // ========================== EVENT LISTENERS ==========================
        btnRefresh.addActionListener(e -> tcpClient.sendCommand("REQ_CLIENT_LIST"));

        btnLive.addActionListener(e -> {
            int idx = clientList.getSelectedIndex();
            if (idx < 0 || currentClients == null || idx >= currentClients.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 máy trạm!");
                return;
            }
            Map<String, Object> c = currentClients.get(idx);
            String id = String.valueOf(c.get("id"));
            String pcName = String.valueOf(c.get("pcName"));
            new LivestreamDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    tcpClient, dispatcher, id, pcName);
        });

        btnSaveFrame.addActionListener(e -> {
            int idx = clientList.getSelectedIndex();
            if (idx < 0 || currentClients == null || idx >= currentClients.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 máy trạm!");
                return;
            }
            Map<String, Object> c = currentClients.get(idx);
            String id = String.valueOf(c.get("id"));
            tcpClient.sendCommand("SAVE_FRAME:" + id);
            lblStatus.setText("Đã gửi lệnh chụp ảnh cho " + c.get("pcName"));
        });

        btnBroadcast.addActionListener(e -> openBroadcastDialog());

        dispatcher.addHandler(this::handleServerMessage);

        tcpClient.sendCommand("REQ_CLIENT_LIST");

        refreshTimer = new javax.swing.Timer(3000, e -> tcpClient.sendCommand("REQ_CLIENT_LIST"));
        refreshTimer.start();
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("RES_CLIENT_LIST:")) {
            String json = message.substring("RES_CLIENT_LIST:".length());
            SwingUtilities.invokeLater(() -> updateClientList(json));
        } else if (message.startsWith("SUSPICIOUS:")) {
            // Định dạng từ server: SUSPICIOUS:clientId|pcName|payload
            String payload = message.substring("SUSPICIOUS:".length());
            SwingUtilities.invokeLater(() -> addSuspiciousAlert(payload));
        }
    }

    /**
     * Thêm cảnh báo hành vi khả nghi vào đầu danh sách (đẩy các thông báo cũ xuống
     * dưới)
     */
    private void addSuspiciousAlert(String rawPayload) {
        // rawPayload ví dụ: 127.0.0.1:53220|YUSIN|RED|Google Chrome
        String[] parts = rawPayload.split("\\|", 4);
        String clientId = parts.length > 0 ? parts[0] : "Chưa rõ";
        String pcName = parts.length > 1 ? parts[1] : "Máy trạm";
        String tabName = "";
        if (parts.length >= 4) {
            tabName = parts[3];
        } else if (parts.length == 3) {
            tabName = parts[2];
        } else {
            tabName = rawPayload;
        }

        String timeStr = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());

        // Tạo Card Cảnh Báo
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(new Color(0x1E, 0x29, 0x3B)); // Navy đậm
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 1, 0, new Color(0xEF, 0x44, 0x44)), // Viền đỏ bên trái
                new EmptyBorder(10, 12, 10, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        // Header card
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblTime = new JLabel("[!] " + timeStr + " - PHÁT HIỆN HÀNH VI KHẢ NGHI");
        lblTime.setFont(LanMonitorTheme.FONT_BOLD);
        lblTime.setForeground(new Color(0xF8, 0x71, 0x71)); // Đỏ sáng

        topRow.add(lblTime, BorderLayout.WEST);
        card.add(topRow, BorderLayout.NORTH);

        // Body card: Thông tin máy & tab mở
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        infoPanel.setOpaque(false);

        JLabel lblMachine = new JLabel("Máy: " + pcName + " | IP/Cổng: " + clientId);
        lblMachine.setFont(LanMonitorTheme.FONT_BASE);
        lblMachine.setForeground(Color.WHITE);

        JLabel lblTab = new JLabel("Đang mở tab/ứng dụng: " + tabName);
        lblTab.setFont(LanMonitorTheme.FONT_BOLD);
        lblTab.setForeground(new Color(0xFB, 0xBF, 0x24)); // Vàng cam cảnh báo

        infoPanel.add(lblMachine);
        infoPanel.add(lblTab);
        card.add(infoPanel, BorderLayout.CENTER);

        // Thêm vào đầu container (Index 0 để đẩy các card cũ xuống dưới)
        alertListContainer.add(card, 0);
        alertListContainer.add(Box.createVerticalStrut(6), 1);
        alertListContainer.revalidate();
        alertListContainer.repaint();

        // Cuộn mượt về đầu danh sách
        SwingUtilities.invokeLater(() -> {
            JScrollBar vbar = alertScrollPane.getVerticalScrollBar();
            if (vbar != null) {
                vbar.setValue(0);
            }
        });
    }

    private void clearAlerts() {
        alertListContainer.removeAll();
        alertListContainer.revalidate();
        alertListContainer.repaint();
    }

    @SuppressWarnings("unchecked")
    private void updateClientList(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> clients = mapper.readValue(json, List.class);
            currentClients = clients;

            int prevSelectedIdx = clientList.getSelectedIndex();
            clientListModel.clear();
            if (clients.isEmpty()) {
                clientListModel.addElement("(Chưa có máy trạm nào kết nối)");
                lblCount.setText("0 máy");
            } else {
                for (Map<String, Object> c : clients) {
                    String id = String.valueOf(c.get("id"));
                    String ip = String.valueOf(c.get("ip"));
                    String pcName = String.valueOf(c.get("pcName"));
                    clientListModel.addElement("Máy: " + pcName + " - IP: " + ip + " [" + id + "]");
                }
                lblCount.setText(clients.size() + " máy");
                if (prevSelectedIdx >= 0 && prevSelectedIdx < clients.size()) {
                    clientList.setSelectedIndex(prevSelectedIdx);
                }
            }
            lblStatus.setText("Cập nhật lúc: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        } catch (Exception e) {
            lblStatus.setText("Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }

    @Override
    public void removeNotify() {
        if (refreshTimer != null) refreshTimer.stop();

        super.removeNotify();
    }

    private void openBroadcastDialog() {
        if (currentClients == null || currentClients.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có máy trạm nào đang kết nối!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
                "Phát thông báo đến máy trạm", true);
        dialog.setSize(450, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement("Tất cả máy trạm");
        for (Map<String, Object> c : currentClients) {
            comboModel.addElement(c.get("pcName") + " [" + c.get("id") + "]");
        }
        JComboBox<String> comboTarget = new JComboBox<>(comboModel);

        JTextArea txtMessage = new JTextArea(5, 30);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        JScrollPane scrollMsg = new JScrollPane(txtMessage);

        JPanel formPanel = new JPanel(new BorderLayout(5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        formPanel.add(new JLabel("Gửi đến:"), BorderLayout.NORTH);
        formPanel.add(comboTarget, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centerPanel.add(new JLabel("Nội dung thông báo:"), BorderLayout.NORTH);
        centerPanel.add(scrollMsg, BorderLayout.CENTER);

        JButton btnSend = LanMonitorTheme.createPrimaryButton("Gửi", LanMonitorTheme.GREEN_BTN);
        JButton btnCancel = LanMonitorTheme.createSecondaryButton("Hủy");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.add(btnCancel);
        btnPanel.add(btnSend);

        dialog.add(formPanel, BorderLayout.NORTH);
        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSend.addActionListener(e -> {
            String content = txtMessage.getText().trim();
            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập nội dung thông báo!",
                        "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int selectedIdx = comboTarget.getSelectedIndex();
            String target;
            if (selectedIdx == 0) {
                target = "ALL";
            } else {
                Map<String, Object> client = currentClients.get(selectedIdx - 1);
                target = String.valueOf(client.get("id"));
            }
            String encoded = java.util.Base64.getEncoder()
                    .encodeToString(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            tcpClient.sendCommand("BROADCAST_MSG:" + target + ":" + encoded);
            lblStatus.setText("Đã gửi thông báo lúc "
                    + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
            dialog.dispose();
        });

        dialog.setVisible(true);
    }
}
