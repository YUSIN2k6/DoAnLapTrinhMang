package com.vku.lanmonitor.admin;

import com.vku.lanmonitor.admin.ui.AdminLoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class AdminApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> new AdminLoginFrame().setVisible(true));
    }
}
