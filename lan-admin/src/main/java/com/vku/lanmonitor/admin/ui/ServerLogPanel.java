package com.vku.lanmonitor.admin.ui;

import com.vku.lanmonitor.admin.ui.theme.LanMonitorTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Giao diện Nhật ký Server (Nền trắng, chữ đen, sạch sẽ và dễ đọc).
 */
public class ServerLogPanel extends JPanel {

    private static final int MAX_LOG_LINES = 1000;
    private final JTextArea logArea;
    private int lineCount;

    public ServerLogPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0xF1, 0xF5, 0xF9)); // Xám sáng thanh lịch
        header.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel titleLabel = new JLabel("NHẬT KÝ SERVER");
        titleLabel.setFont(LanMonitorTheme.FONT_BOLD);
        titleLabel.setForeground(new Color(0x33, 0x41, 0x55));
        header.add(titleLabel, BorderLayout.WEST);

        JButton clearBtn = LanMonitorTheme.createSecondaryButton("Xóa nhật ký");
        clearBtn.setPreferredSize(new Dimension(110, 30));
        clearBtn.addActionListener(e -> clear());
        header.add(clearBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Nền trắng chữ đen
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(new Color(0x1E, 0x29, 0x3B)); // Chữ đen đậm nét
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setCaretColor(Color.BLACK);
        logArea.setMargin(new Insets(10, 14, 10, 14));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xCB, 0xD5, 0xE1)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendLine(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            lineCount++;
            if (lineCount > MAX_LOG_LINES) {
                trimLog();
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            logArea.setText("");
            lineCount = 0;
        });
    }

    private void trimLog() {
        String text = logArea.getText();
        int cutoff = 0;
        int linesToRemove = lineCount - MAX_LOG_LINES;
        for (int i = 0; i < linesToRemove && cutoff < text.length(); i++) {
            int idx = text.indexOf('\n', cutoff);
            if (idx < 0)
                break;
            cutoff = idx + 1;
        }
        logArea.setText(text.substring(cutoff));
        lineCount = MAX_LOG_LINES;
    }
}
