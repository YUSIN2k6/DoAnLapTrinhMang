package com.vku.lanmonitor.student.ui;

import com.vku.lanmonitor.student.net.StudentTcpClient;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField txtStudentId;
    private JPasswordField txtPassword;
    private JLabel lblServerInfo;
    private JButton btnLogin;
    private JLabel lblStatus;
    
    private StudentTcpClient tcpClient;
    private String serverHost = "localhost";
    private int serverPort = 9999;

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Đăng nhập - LAN Monitor");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        tcpClient = new StudentTcpClient();
        tcpClient.setMessageListener(this::handleServerMessage);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel lblTitle = new JLabel("Đăng nhập sinh viên");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(lblTitle);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel panelStudentId = new JPanel(new BorderLayout(5, 5));
        panelStudentId.add(new JLabel("Mã Sinh Viên:"), BorderLayout.NORTH);
        txtStudentId = new JTextField(20);
        panelStudentId.add(txtStudentId, BorderLayout.CENTER);
        mainPanel.add(panelStudentId);
        mainPanel.add(Box.createVerticalStrut(15));

        JPanel panelPassword = new JPanel(new BorderLayout(5, 5));
        panelPassword.add(new JLabel("Mật khẩu:"), BorderLayout.NORTH);
        txtPassword = new JPasswordField(20);
        panelPassword.add(txtPassword, BorderLayout.CENTER);
        mainPanel.add(panelPassword);
        mainPanel.add(Box.createVerticalStrut(15));

        lblServerInfo = new JLabel("Server: " + serverHost + ":" + serverPort);
        lblServerInfo.setFont(new Font("Arial", Font.ITALIC, 12));
        lblServerInfo.setForeground(Color.GRAY);
        lblServerInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(lblServerInfo);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnLogin = new JButton("Đăng nhập");
        
        btnLogin.addActionListener(e -> handleLogin());
        
        buttonPanel.add(btnLogin);
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(lblStatus);

        add(mainPanel);

        txtStudentId.addActionListener(e -> txtPassword.requestFocus());
        txtPassword.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String studentId = txtStudentId.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (studentId.isEmpty()) {
            showStatus("Vui lòng nhập mã sinh viên!", Color.RED);
            txtStudentId.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showStatus("Vui lòng nhập mật khẩu!", Color.RED);
            txtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        if (!tcpClient.isConnected()) {
            try {
                tcpClient.connect(serverHost, serverPort);
            } catch (Exception e) {
                showStatus("Không thể kết nối server: " + e.getMessage(), Color.RED);
                btnLogin.setEnabled(true);
                return;
            }
        }

        showStatus("Đang đăng nhập...", Color.BLUE);
        tcpClient.sendCommand("STUDENT_LOGIN:" + studentId + ":" + password);
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("LOGIN_OK")) {
                showStatus("Đăng nhập thành công!", new Color(0, 150, 0));
                openStudentHome(txtStudentId.getText().trim());
            } else if (message.startsWith("LOGIN_FAIL")) {
                showStatus("Đăng nhập thất bại!", Color.RED);
                btnLogin.setEnabled(true);
            } else if (message.startsWith("ERROR:")) {
                showStatus(message.substring(6), Color.RED);
                btnLogin.setEnabled(true);
            }
        });
    }

    private void openStudentHome(String studentId) {
        System.out.println("[DEBUG] openStudentHome: " + studentId);
        SwingUtilities.invokeLater(() -> {
            try {
                new StudentHomeFrame(studentId, tcpClient);
                System.out.println("[DEBUG] StudentHomeFrame created");
                setVisible(false);
                // KHÔNG disconnect tcpClient
                // KHÔNG dispose
            } catch (Exception e) {
                System.err.println("[DEBUG] LỖI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showStatus(String message, Color color) {
        lblStatus.setText(message);
        lblStatus.setForeground(color);
    }
}
