package com.vku.lanmonitor.admin.ui;

import com.vku.lanmonitor.admin.net.AdminTcpClient;
import com.vku.lanmonitor.admin.service.ProcessManager;
import com.vku.lanmonitor.admin.ui.theme.LanMonitorTheme;

import javax.swing.*;
import java.awt.*; // includes GraphicsEnvironment and Rectangle
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Admin Login Frame with integrated Server control and real-time status check.
 * UI Theme: Dragon Ball Z — Son Goku Power Palette.
 */
public class AdminLoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtServerHost;
    private JTextField txtServerPort;
    private JButton btnLogin;
    private JButton btnStartServer;
    private JLabel lblStatus;
    private JLabel serverStatusLabel;
    private AdminTcpClient tcpClient;
    private final ProcessManager processManager;
    private javax.swing.Timer statusCheckTimer;

    public AdminLoginFrame() {
        this.processManager = new ProcessManager();
        initComponents();
        initServerStatusCheck();
    }

    private void initComponents() {
        setTitle("Đăng nhập Giám thị - LAN Monitor");
        setSize(450, 490);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                Rectangle bounds = env.getMaximumWindowBounds();
                setMaximizedBounds(bounds);
                setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (tcpClient != null) {
                    tcpClient.disconnect();
                }
                processManager.stopAll();
                dispose();
                System.exit(0);
            }
        });

        // ============================================================
        // GOKU THEME — Card bọc form (viền vàng Super Saiyan Gold)
        // ============================================================
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setMaximumSize(new Dimension(420, 580));

        // Card viền vàng bọc form
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(LanMonitorTheme.DARK_CARD);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(LanMonitorTheme.DARK_BORDER, 2),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)));
        cardPanel.setMaximumSize(new Dimension(420, 580));

        // ---- Logo / Tiêu đề — Power Level style ----
        JLabel lblTitle = new JLabel("LAN MONITOR", SwingConstants.CENTER);
        lblTitle.setFont(LanMonitorTheme.FONT_GIANT);
        lblTitle.setForeground(LanMonitorTheme.PURPLE_HEADER); // Cam Goku
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("CONTROL CENTER - ADMIN POWER", SwingConstants.CENTER);
        lblSub.setFont(LanMonitorTheme.FONT_SMALL);
        lblSub.setForeground(LanMonitorTheme.TEXT_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(lblTitle);
        cardPanel.add(lblSub);
        cardPanel.add(Box.createVerticalStrut(16));

        // ---- Server Status Panel ----
        JPanel serverStatusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        serverStatusPanel.setOpaque(false);
        serverStatusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverStatusLabel = new JLabel("Server: OFFLINE");
        serverStatusLabel.setFont(LanMonitorTheme.FONT_BOLD);
        serverStatusLabel.setForeground(LanMonitorTheme.RED_BTN);
        serverStatusPanel.add(serverStatusLabel);
        cardPanel.add(serverStatusPanel);
        cardPanel.add(Box.createVerticalStrut(10));

        // ---- Start Server Button — Energy Green Ki ----
        btnStartServer = LanMonitorTheme.createKiButton(
                "KHOI DONG SERVER",
                LanMonitorTheme.GREEN_BTN,
                new Color(0x16, 0xA3, 0x4A));
        btnStartServer.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStartServer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnStartServer.addActionListener(e -> startServerFromLogin());
        cardPanel.add(btnStartServer);
        cardPanel.add(Box.createVerticalStrut(16));

        // ---- Username ----
        JPanel panelUser = new JPanel(new BorderLayout(5, 3));
        panelUser.setOpaque(false);
        JLabel lblUser = new JLabel("T\u00E0i kho\u1EA3n:");
        lblUser.setFont(LanMonitorTheme.FONT_BOLD);
        lblUser.setForeground(LanMonitorTheme.DARK_BORDER);
        panelUser.add(lblUser, BorderLayout.NORTH);
        txtUsername = new JTextField("admin", 20);
        styleKiField(txtUsername);
        panelUser.add(txtUsername, BorderLayout.CENTER);
        cardPanel.add(panelUser);
        cardPanel.add(Box.createVerticalStrut(10));

        // ---- Password ----
        JPanel panelPass = new JPanel(new BorderLayout(5, 3));
        panelPass.setOpaque(false);
        JLabel lblPass = new JLabel("M\u1EADt kh\u1EA9u:");
        lblPass.setFont(LanMonitorTheme.FONT_BOLD);
        lblPass.setForeground(LanMonitorTheme.DARK_BORDER);
        panelPass.add(lblPass, BorderLayout.NORTH);
        txtPassword = new JPasswordField("123456", 20);
        styleKiField(txtPassword);
        panelPass.add(txtPassword, BorderLayout.CENTER);
        cardPanel.add(panelPass);
        cardPanel.add(Box.createVerticalStrut(10));

        // ---- Server Host ----
        JPanel panelHost = new JPanel(new BorderLayout(5, 3));
        panelHost.setOpaque(false);
        JLabel lblHost = new JLabel("Server IP:");
        lblHost.setFont(LanMonitorTheme.FONT_BOLD);
        lblHost.setForeground(LanMonitorTheme.DARK_BORDER);
        panelHost.add(lblHost, BorderLayout.NORTH);
        txtServerHost = new JTextField("localhost", 20);
        styleKiField(txtServerHost);
        panelHost.add(txtServerHost, BorderLayout.CENTER);
        cardPanel.add(panelHost);
        cardPanel.add(Box.createVerticalStrut(10));

        // ---- Server Port ----
        JPanel panelPort = new JPanel(new BorderLayout(5, 3));
        panelPort.setOpaque(false);
        JLabel lblPort = new JLabel("Server Port:");
        lblPort.setFont(LanMonitorTheme.FONT_BOLD);
        lblPort.setForeground(LanMonitorTheme.DARK_BORDER);
        panelPort.add(lblPort, BorderLayout.NORTH);
        txtServerPort = new JTextField("9999", 20);
        styleKiField(txtServerPort);
        panelPort.add(txtServerPort, BorderLayout.CENTER);
        cardPanel.add(panelPort);
        cardPanel.add(Box.createVerticalStrut(20));

        // ---- Button Login — Kamehameha Orange Gradient ----
        btnLogin = LanMonitorTheme.createKiButton(
                "DANG NHAP",
                LanMonitorTheme.PURPLE_HEADER,
                new Color(0xEA, 0x58, 0x0C));
        btnLogin.setFont(LanMonitorTheme.FONT_BOLD.deriveFont(15f));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.addActionListener(e -> handleLogin());
        cardPanel.add(btnLogin);
        cardPanel.add(Box.createVerticalStrut(12));

        // ---- Status label ----
        lblStatus = new JLabel(" ");
        lblStatus.setFont(LanMonitorTheme.FONT_SMALL);
        lblStatus.setForeground(LanMonitorTheme.TEXT_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(lblStatus);

        mainPanel.add(cardPanel);

        // ============================================================
        // Wrapper với nền gradient tối Saiyan Arena
        // ============================================================
        JPanel wrapperPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x0F, 0x17, 0x2A),
                        getWidth(), getHeight(), new Color(0x1E, 0x3A, 0x5F));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        wrapperPanel.setDoubleBuffered(true);
        wrapperPanel.add(mainPanel);
        add(wrapperPanel);

        // Enter key shortcuts
        txtUsername.addActionListener(e -> txtPassword.requestFocus());
        txtPassword.addActionListener(e -> handleLogin());
        txtServerHost.addActionListener(e -> handleLogin());
        txtServerPort.addActionListener(e -> handleLogin());
    }

    /** Ki Input Field style: nền navy tối, chữ trắng ngà, viền vàng SS. */
    private void styleKiField(JTextField field) {
        field.setBackground(new Color(0x1E, 0x3A, 0x5F));
        field.setForeground(LanMonitorTheme.TEXT_LIGHT);
        field.setCaretColor(LanMonitorTheme.DARK_BORDER);
        field.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(LanMonitorTheme.DARK_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        field.setFont(LanMonitorTheme.FONT_BASE);
        field.setOpaque(true);
    }

    private void initServerStatusCheck() {
        statusCheckTimer = new javax.swing.Timer(2000, e -> refreshServerStatus());
        statusCheckTimer.start();
    }

    private void refreshServerStatus() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return processManager.isPortInUse(9999);
            }

            @Override
            protected void done() {
                try {
                    boolean online = get();
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText(online ? "Server: ONLINE" : "Server: OFFLINE");
                        serverStatusLabel.setForeground(online ? LanMonitorTheme.GREEN_BTN : LanMonitorTheme.RED_BTN);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("[AdminLoginFrame] Status check interrupted: " + e.getMessage());
                } catch (ExecutionException e) {
                    System.err.println("[AdminLoginFrame] Status check failed: "
                            + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                }
            }
        }.execute();
    }

    private void startServerFromLogin() {
        btnStartServer.setEnabled(false);
        btnStartServer.setText("Đang khởi động...");
        new Thread(() -> {
            try {
                processManager.startServer();
                javax.swing.Timer checkTimer = new javax.swing.Timer(5000, evt -> {
                    new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() {
                            return processManager.isPortInUse(9999);
                        }

                        @Override
                        protected void done() {
                            try {
                                boolean online = get();
                                SwingUtilities.invokeLater(() -> {
                                    serverStatusLabel.setText(online ? "Server: ONLINE" : "Server: OFFLINE");
                                    serverStatusLabel.setForeground(
                                            online ? LanMonitorTheme.GREEN_BTN : LanMonitorTheme.RED_BTN);
                                    btnStartServer.setEnabled(true);
                                    btnStartServer.setText("KHOI DONG SERVER");
                                    if (online) {
                                        enableLoginButton();
                                    }
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                System.err.println("[AdminLoginFrame] Port check interrupted: " + e.getMessage());
                                resetStartButton();
                            } catch (ExecutionException e) {
                                System.err.println("[AdminLoginFrame] Port check failed: "
                                        + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                                resetStartButton();
                            }
                        }
                    }.execute();
                });
                checkTimer.setRepeats(false);
                checkTimer.start();
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi khởi động server: " + ex.getMessage(), "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    resetStartButton();
                });
            }
        }, "LoginServerStartThread").start();
    }

    private void resetStartButton() {
        SwingUtilities.invokeLater(() -> {
            btnStartServer.setEnabled(true);
            btnStartServer.setText("KHOI DONG SERVER");
        });
    }

    private void handleLogin() {
        if (!btnLogin.isEnabled()) {
            return;
        }
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String host = txtServerHost.getText().trim();
        String portStr = txtServerPort.getText().trim();

        if (username.isEmpty()) {
            showStatus("Vui lòng nhập tài khoản!", LanMonitorTheme.RED_BTN);
            txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showStatus("Vui lòng nhập mật khẩu!", LanMonitorTheme.RED_BTN);
            txtPassword.requestFocus();
            return;
        }
        if (host.isEmpty()) {
            showStatus("Vui lòng nhập Server IP!", LanMonitorTheme.RED_BTN);
            txtServerHost.requestFocus();
            return;
        }
        if (portStr.isEmpty()) {
            showStatus("Vui lòng nhập Server Port!", LanMonitorTheme.RED_BTN);
            txtServerPort.requestFocus();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            showStatus("Port phải là số nguyên!", LanMonitorTheme.RED_BTN);
            txtServerPort.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        showStatus("Đang đăng nhập...", LanMonitorTheme.BLUE_BTN);

        new Thread(() -> {
            try {
                if (tcpClient == null || !tcpClient.isConnected()) {
                    tcpClient = new AdminTcpClient();
                    tcpClient.setMessageListener(this::handleServerMessage);
                    tcpClient.connect(host, port);
                    Thread.sleep(500);
                }
                tcpClient.sendCommand("ADMIN_LOGIN:" + username + ":" + password);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    showStatus("Không thể kết nối server: " + ex.getMessage(), LanMonitorTheme.RED_BTN);
                    btnLogin.setEnabled(true);
                });
            }
        }).start();
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("ADMIN_OK")) {
                showStatus("Đăng nhập thành công!", LanMonitorTheme.GREEN_BTN);
                dispose();
                new AdminMainFrame(tcpClient);
            } else if (message.startsWith("ADMIN_FAIL")) {
                showStatus("Sai tài khoản hoặc mật khẩu", LanMonitorTheme.RED_BTN);
                btnLogin.setEnabled(true);
            } else if (message.startsWith("ERROR:")) {
                showStatus(message.substring(6), LanMonitorTheme.RED_BTN);
                btnLogin.setEnabled(true);
            }
        });
    }

    private void enableLoginButton() {
        btnLogin.setEnabled(true);
        lblStatus.setText("Server đã sẵn sàng. Bạn có thể đăng nhập.");
        lblStatus.setForeground(LanMonitorTheme.GREEN_BTN);
    }

    private void showStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }
}
