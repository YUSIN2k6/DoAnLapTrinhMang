package com.vku.lanmonitor.admin.ui;

import com.vku.lanmonitor.admin.net.AdminMessageDispatcher;
import com.vku.lanmonitor.admin.net.AdminTcpClient;
import com.vku.lanmonitor.admin.service.ProcessManager;
import com.vku.lanmonitor.admin.ui.theme.LanMonitorTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main Admin Dashboard frame with streamlined header controls,
 * popup server logs, and dual-column dashboard.
 */
public class AdminMainFrame extends JFrame {

    private final AdminTcpClient tcpClient;
    private final AdminMessageDispatcher dispatcher;
    private final ProcessManager processManager;
    private final ServerLogPanel logPanel;

    private JLabel statusBadge;
    private JButton btnStartServer;
    private JButton btnStopServer;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    public AdminMainFrame(AdminTcpClient tcpClient) {
        super("Admin Dashboard - LAN Monitor");
        this.tcpClient = tcpClient;
        this.dispatcher = new AdminMessageDispatcher();
        tcpClient.setMessageListener(dispatcher::dispatch);

        this.processManager = new ProcessManager();
        this.logPanel = new ServerLogPanel();

        processManager.setLogListener(line -> {
            logPanel.appendLine(line);
        });

        // Lắng nghe log trực tiếp từ Server qua socket khi chạy từ RunServer.bat
        dispatcher.addHandler(msg -> {
            if (msg.startsWith("SERVER_LOG:")) {
                String logText = msg.substring("SERVER_LOG:".length());
                logPanel.appendLine(logText);
            }
        });

        LanMonitorTheme.applyGlobalStyle();

        setLayout(new BorderLayout());
        initComponents();

        setSize(1520, 900);
        setMinimumSize(new Dimension(1200, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        setVisible(true);

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    AdminMainFrame.this,
                    "Chào mừng Admin đã quay trở lại!",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void initComponents() {
        // ---- Title Bar ----
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setPreferredSize(new Dimension(1520, 28));
        titleBar.setBackground(new Color(0x0F, 0x17, 0x2A));

        JPanel iconPanel = new JPanel();
        iconPanel.setBackground(LanMonitorTheme.PURPLE_HEADER);
        iconPanel.setPreferredSize(new Dimension(16, 16));
        JLabel iconLbl = new JLabel("LM", SwingConstants.CENTER);
        iconLbl.setForeground(Color.WHITE);
        iconLbl.setFont(LanMonitorTheme.FONT_SMALL);
        iconPanel.add(iconLbl);

        JLabel titleLbl = new JLabel("  Admin Dashboard - LAN Monitor");
        titleLbl.setFont(LanMonitorTheme.FONT_BASE);
        titleLbl.setForeground(LanMonitorTheme.TEXT_LIGHT);

        JPanel westTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 6));
        westTitle.setOpaque(false);
        westTitle.add(iconPanel);
        westTitle.add(titleLbl);
        titleBar.add(westTitle, BorderLayout.WEST);

        // ---- Header Panel ----
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, LanMonitorTheme.PURPLE_HEADER,
                        getWidth(), 0, new Color(0xEA, 0x58, 0x0C));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        headerPanel.setPreferredSize(new Dimension(1520, 68));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        headerPanel.setDoubleBuffered(true);

        // Header West: Logo & Title
        JPanel headerWest = new JPanel(new GridLayout(2, 1));
        headerWest.setOpaque(false);
        JLabel mainTitle = new JLabel("LAN MONITOR");
        mainTitle.setFont(LanMonitorTheme.FONT_TITLE.deriveFont(Font.BOLD, 22f));
        mainTitle.setForeground(Color.WHITE);
        JLabel subTitle = new JLabel("HỆ THỐNG GIÁM SÁT & QUẢN LÝ PHÒNG MÁY LAN");
        subTitle.setFont(LanMonitorTheme.FONT_SMALL.deriveFont(11f));
        subTitle.setForeground(new Color(0xFF, 0xED, 0xD5));
        headerWest.add(mainTitle);
        headerWest.add(subTitle);
        headerPanel.add(headerWest, BorderLayout.WEST);

        // Header East: SERVER Controls, Status Badge, Log button, Logout button
        JPanel headerEast = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        headerEast.setOpaque(false);

        // Server Badge
        JLabel lblServerTag = new JLabel("MÁY CHỦ (CỔNG 9999):");
        lblServerTag.setFont(LanMonitorTheme.FONT_BOLD);
        lblServerTag.setForeground(Color.WHITE);

        statusBadge = new JLabel("ĐANG TẮT", SwingConstants.CENTER);
        statusBadge.setOpaque(true);
        statusBadge.setBackground(LanMonitorTheme.RED_BTN);
        statusBadge.setForeground(Color.WHITE);
        statusBadge.setFont(LanMonitorTheme.FONT_BOLD);
        statusBadge.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(LanMonitorTheme.DARK_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        btnStartServer = LanMonitorTheme.createPrimaryButton("Bật Server", LanMonitorTheme.GREEN_BTN);
        btnStartServer.addActionListener(e -> startServerAction());

        btnStopServer = LanMonitorTheme.createPrimaryButton("Tắt Server", LanMonitorTheme.RED_BTN);
        btnStopServer.setEnabled(false);
        btnStopServer.addActionListener(e -> stopServerAction());

        JButton btnShowLog = LanMonitorTheme.createSecondaryButton("Nhật ký server");
        btnShowLog.addActionListener(e -> openLogDialog());

        JButton btnLogout = LanMonitorTheme.createKiButton(
                "ĐĂNG XUẤT",
                LanMonitorTheme.RED_BTN,
                new Color(0xB9, 0x1C, 0x1C));
        btnLogout.addActionListener(e -> handleLogout());

        headerEast.add(lblServerTag);
        headerEast.add(statusBadge);
        headerEast.add(btnStartServer);
        headerEast.add(btnStopServer);
        headerEast.add(btnShowLog);
        headerEast.add(btnLogout);
        headerPanel.add(headerEast, BorderLayout.EAST);

        // ---- Menu Bar ----
        JPanel menuBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        menuBar.setBackground(new Color(0x1E, 0x3A, 0x5F));
        menuBar.setPreferredSize(new Dimension(1520, 30));

        JLabel fileLabel = new JLabel("  Menu:  ");
        fileLabel.setFont(LanMonitorTheme.FONT_BASE);
        fileLabel.setForeground(LanMonitorTheme.TEXT_MUTED);
        menuBar.add(fileLabel);

        menuBar.add(createTabButton("Bảng giám sát", "dashboard"));
        menuBar.add(createTabButton("Quản lý đề thi", "quan-ly-de-thi"));
        menuBar.add(createTabButton("Cấu hình hệ thống", "cau-hinh"));

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(titleBar, BorderLayout.NORTH);
        topContainer.add(headerPanel, BorderLayout.CENTER);
        topContainer.add(menuBar, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // ---- Main Workspace ----
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        DashboardPanel dashboardPanel = new DashboardPanel(tcpClient, dispatcher);
        QuizEditorPanel quizEditorPanel = new QuizEditorPanel(tcpClient, dispatcher);
        ConfigPanel configPanel = new ConfigPanel(tcpClient, dispatcher);

        contentPanel.add(dashboardPanel, "dashboard");
        contentPanel.add(quizEditorPanel, "quan-ly-de-thi");
        contentPanel.add(configPanel, "cau-hinh");

        add(contentPanel, BorderLayout.CENTER);

        // ---- Status Bar ----
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(0x0F, 0x17, 0x2A));
        statusBar.setPreferredSize(new Dimension(1520, 24));

        JLabel leftStatus = new JLabel("  Admin: admin | Kết nối Hub: 127.0.0.1:9999");
        leftStatus.setFont(LanMonitorTheme.FONT_SMALL);
        leftStatus.setForeground(LanMonitorTheme.TEXT_MUTED);
        statusBar.add(leftStatus, BorderLayout.WEST);

        JPanel statusEast = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        statusEast.setOpaque(false);
        JLabel timeLbl = new JLabel();
        timeLbl.setFont(LanMonitorTheme.FONT_SMALL);
        timeLbl.setForeground(LanMonitorTheme.BLUE_BTN);
        JLabel pathLbl = new JLabel(ProcessManager.getBaseDir());
        pathLbl.setFont(LanMonitorTheme.FONT_SMALL);
        pathLbl.setForeground(LanMonitorTheme.TEXT_MUTED);
        statusEast.add(timeLbl);
        statusEast.add(pathLbl);
        statusBar.add(statusEast, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);

        // Timer kiểm tra trạng thái Server cổng 9999
        new javax.swing.Timer(2000, e -> {
            new Thread(() -> {
                boolean online = processManager.isPortInUse(9999);
                SwingUtilities.invokeLater(() -> {
                    statusBadge.setText(online ? "HOẠT ĐỘNG" : "ĐANG TẮT");
                    statusBadge.setBackground(online ? LanMonitorTheme.GREEN_BTN : LanMonitorTheme.RED_BTN);
                    btnStartServer.setEnabled(!online);
                    btnStopServer.setEnabled(online);
                });
            }, "StatusCheckThread").start();
        }).start();

        new Timer(1000,
                e -> timeLbl.setText("Thời gian: " + new SimpleDateFormat("HH:mm:ss").format(new Date())))
                .start();
    }

    private void startServerAction() {
        btnStartServer.setEnabled(false);
        new Thread(() -> {
            try {
                processManager.startServer();
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi bật server: " + ex.getMessage(), "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    btnStartServer.setEnabled(true);
                });
            }
        }).start();
    }

    private void stopServerAction() {
        btnStopServer.setEnabled(false);
        processManager.stopServer();
    }

    private void openLogDialog() {
        JDialog logDialog = new JDialog(this, "Nhật ký server", false);
        logDialog.setSize(950, 580);
        logDialog.setLocationRelativeTo(this);
        logDialog.setLayout(new BorderLayout());
        logDialog.add(logPanel, BorderLayout.CENTER);
        logDialog.setVisible(true);
    }

    private JButton createTabButton(String text, String id) {
        JButton btn = new JButton(text);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(LanMonitorTheme.FONT_BASE);
        btn.setForeground(LanMonitorTheme.TEXT_LIGHT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(LanMonitorTheme.DARK_BORDER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
               }

    
        });
        btn.addActionListener(e -> switchTab(id));
        return btn;
    }

    private void switchTab(String tabId) {
        cardLayout.show(contentPanel, tabId);
    }

    private void handleExit() {
        new Thread(() -> {
            try {
                if (tcpClient != null) {
                    tcpClient.disconnect();
                }
            } catch (Exception ignored) {
            }
            try {
                processManager.stopAll();
            } catch (Exception ignored) {
            }
            System.exit(0);
        }).start();
        dispose();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (tcpClient != null) {
                tcpClient.disconnect();
            }
            processManager.stopAll();
            dispose();
            new AdminLoginFrame().setVisible(true);
        }
    }
}
