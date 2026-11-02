package com.vku.lanmonitor.admin.ui.theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Design System for LAN Monitor Admin.
 * Giao diện hiện đại, nút bấm bo góc êm ái, màu sắc hài hòa và padding rộng
 * rãi.
 */
public final class LanMonitorTheme {

    // Bảng màu hiện đại
    public static final Color PURPLE_HEADER = new Color(0x25, 0x63, 0xEB); // Royal Blue
    public static final Color PURPLE_LIGHT = new Color(0xDB, 0xEA, 0xFE);
    public static final Color DARK_SIDEBAR = new Color(0x0F, 0x17, 0x2A); // Slate Dark
    public static final Color DARK_CARD = new Color(0x1E, 0x29, 0x3B);
    public static final Color DARK_CARD_HOVER = new Color(0x33, 0x41, 0x55);
    public static final Color DARK_BORDER = new Color(0x38, 0xBD, 0xF8); // Sky Blue Accent
    public static final Color LIGHT_BG = new Color(0xF8, 0xFA, 0xFC); // Nền sáng sạch sẽ
    public static final Color LIGHT_CONTENT = new Color(0xE2, 0xE8, 0xF0);
    public static final Color WHITE = Color.WHITE;
    public static final Color LOG_BG = Color.WHITE;
    public static final Color LOG_TEXT = new Color(0x0F, 0x17, 0x2A);
    public static final Color GREEN_BTN = new Color(0x16, 0xA3, 0x4A); // Xanh lá tươi
    public static final Color RED_BTN = new Color(0xDC, 0x26, 0x26); // Đỏ sang
    public static final Color BLUE_BTN = new Color(0x25, 0x63, 0xEB); // Xanh dương
    public static final Color ORANGE_BTN = new Color(0xEA, 0x58, 0x0C); // Cam ấm
    public static final Color TEXT_LIGHT = new Color(0xF8, 0xFA, 0xFC);
    public static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);

    // Phông chữ Segoe UI
    public static final Font FONT_BASE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FONT_GIANT = new Font("Segoe UI", Font.BOLD, 26);

    private LanMonitorTheme() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Tạo nút chính có bo góc, đổ bóng nhẹ và padding rộng không bị chật viền
     */
    public static JButton createPrimaryButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? bg : new Color(0x94, 0xA3, 0xB8));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(7, 16, 7, 16)); // Padding rộng rãi
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Tạo nút phụ viền bo thanh lịch
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xCB, 0xD5, 0xE1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BASE);
        btn.setForeground(new Color(0x1E, 0x29, 0x3B));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createSecondaryButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createKiButton(String text, Color colorStart, Color colorEnd) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, colorStart, getWidth(), getHeight(), colorEnd);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JPanel createDarkCard() {
        JPanel panel = new JPanel();
        panel.setBackground(DARK_CARD);
        panel.setBorder(new LineBorder(DARK_BORDER));
        return panel;
    }

    public static void applyGlobalStyle() {
        UIManager.put("Button.font", FONT_BASE);
        UIManager.put("Label.font", FONT_BASE);
        UIManager.put("TextField.font", FONT_BASE);
        UIManager.put("TextArea.font", FONT_MONO);
    }
}
