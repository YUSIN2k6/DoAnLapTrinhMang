package com.vku.lanmonitor.admin.ui;

import com.vku.lanmonitor.admin.net.AdminMessageDispatcher;
import com.vku.lanmonitor.admin.net.AdminTcpClient;
import com.vku.lanmonitor.admin.ui.theme.LanMonitorTheme;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Configuration panel for managing the application whitelist.
 * Uses {@link LanMonitorTheme} for consistent styling.
 */
public class ConfigPanel extends JPanel {

    private final AdminTcpClient tcpClient;
    private final JTextArea txtWhitelist;
    private final JLabel lblStatus;

    public ConfigPanel(AdminTcpClient tcpClient, AdminMessageDispatcher dispatcher) {
        this.tcpClient = tcpClient;
        setLayout(new BorderLayout(5, 5));
        setBackground(LanMonitorTheme.LIGHT_BG);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("Cau hinh Whitelist - Danh sach app duoc phep");
        lblTitle.setFont(LanMonitorTheme.FONT_TITLE);
        add(lblTitle, BorderLayout.NORTH);

        txtWhitelist = new JTextArea();
        txtWhitelist.setFont(LanMonitorTheme.FONT_MONO);
        txtWhitelist.setLineWrap(true);
        txtWhitelist.setWrapStyleWord(true);
        txtWhitelist.setBackground(LanMonitorTheme.WHITE);
        JScrollPane scroll = new JScrollPane(txtWhitelist);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LanMonitorTheme.LIGHT_CONTENT),
                BorderFactory.createTitledBorder("Nhap cac keyword, cach nhau boi dau phay (,)")));
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        south.setOpaque(false);
        JButton btnLoad = LanMonitorTheme.createSecondaryButton("Tai lai");
        JButton btnSave = LanMonitorTheme.createPrimaryButton("Luu & Ap dung", LanMonitorTheme.GREEN_BTN);

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(LanMonitorTheme.BLUE_BTN);
        lblStatus.setFont(LanMonitorTheme.FONT_SMALL);
        lblStatus.setPreferredSize(new Dimension(200, 25));
        south.add(btnLoad);
        south.add(btnSave);
        south.add(lblStatus);
        add(south, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> tcpClient.sendCommand("REQ_WHITELIST"));
        btnSave.addActionListener(e -> saveWhitelist());

        dispatcher.addHandler(this::handleServerMessage);
        tcpClient.sendCommand("REQ_WHITELIST");
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("RES_WHITELIST:")) {
            String csv = message.substring("RES_WHITELIST:".length());
            SwingUtilities.invokeLater(() -> {
                txtWhitelist.setText(csv);
                lblStatus.setText("Da tai " + csv.split(",").length + " muc");
                lblStatus.setForeground(LanMonitorTheme.BLUE_BTN);
            });
        } else if (message.startsWith("UPDATE_WHITELIST_OK")) {
            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("Da luu thanh cong");
                lblStatus.setForeground(LanMonitorTheme.GREEN_BTN);
            });
        } else if (message.startsWith("UPDATE_WHITELIST_FAIL")) {
            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("Loi: " + message.substring(22));
                lblStatus.setForeground(LanMonitorTheme.RED_BTN);
            });
        }
    }

    private void saveWhitelist() {
        String text = txtWhitelist.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Danh sach khong duoc de trong!");
            return;
        }
        String csv = String.join(",", Arrays.stream(text.split("[,\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new));
        int c = JOptionPane.showConfirmDialog(this,
                "Luu va ap dung whitelist?\nNoi dung:\n" + csv,
                "Xac nhan", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            tcpClient.sendCommand("UPDATE_WHITELIST_CONFIG:" + csv);
        }
    }
}
