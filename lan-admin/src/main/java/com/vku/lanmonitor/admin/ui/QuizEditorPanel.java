package com.vku.lanmonitor.admin.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vku.lanmonitor.admin.net.AdminMessageDispatcher;
import com.vku.lanmonitor.admin.net.AdminTcpClient;
import com.vku.lanmonitor.admin.ui.theme.LanMonitorTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Quiz management panel with themed table and action buttons.
 * Uses {@link LanMonitorTheme} for consistent styling.
 */
public class QuizEditorPanel extends JPanel {

    private final AdminTcpClient tcpClient;
    private final JTable quizTable;
    private final DefaultTableModel tableModel;
    private final JLabel lblStatus;
    private List<Map<String, Object>> quizzesData;

    public QuizEditorPanel(AdminTcpClient tcpClient, AdminMessageDispatcher dispatcher) {
        this.tcpClient = tcpClient;
        setLayout(new BorderLayout(5, 5));
        setBackground(LanMonitorTheme.LIGHT_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        north.setOpaque(false);
        JLabel lblTitle = new JLabel("Quan ly de thi");
        lblTitle.setFont(LanMonitorTheme.FONT_TITLE);

        JButton btnCreate = LanMonitorTheme.createPrimaryButton("Tao de moi", LanMonitorTheme.GREEN_BTN);
        JButton btnEdit = LanMonitorTheme.createSecondaryButton("Sua");
        JButton btnDelete = LanMonitorTheme.createPrimaryButton("Xoa", LanMonitorTheme.RED_BTN);
        JButton btnRefresh = LanMonitorTheme.createSecondaryButton("Lam moi");

        north.add(lblTitle);
        north.add(Box.createHorizontalStrut(20));
        north.add(btnCreate);
        north.add(btnEdit);
        north.add(btnDelete);
        north.add(btnRefresh);
        add(north, BorderLayout.NORTH);

        String[] columns = {"ID", "Ten de thi", "Thoi gian (phut)", "So cau", "Trang thai"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        quizTable = new JTable(tableModel);
        quizTable.setFont(LanMonitorTheme.FONT_BASE);
        quizTable.setRowHeight(28);
        quizTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        quizTable.setGridColor(LanMonitorTheme.LIGHT_CONTENT);
        quizTable.getTableHeader().setFont(LanMonitorTheme.FONT_BOLD);
        quizTable.getTableHeader().setBackground(LanMonitorTheme.DARK_BORDER);
        quizTable.getTableHeader().setForeground(LanMonitorTheme.DARK_SIDEBAR);
        JScrollPane scroll = new JScrollPane(quizTable);
        scroll.setBorder(BorderFactory.createLineBorder(LanMonitorTheme.LIGHT_CONTENT));
        quizTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    setBackground(LanMonitorTheme.PURPLE_HEADER);
                    setForeground(java.awt.Color.WHITE);
                } else if (row % 2 == 0) {
                    setBackground(new java.awt.Color(0x1E, 0x3A, 0x5F));
                    setForeground(LanMonitorTheme.TEXT_LIGHT);
                } else {
                    setBackground(new java.awt.Color(0x2D, 0x4F, 0x7F));
                    setForeground(LanMonitorTheme.TEXT_LIGHT);
                }
                return this;
            }
        });
        add(scroll, BorderLayout.CENTER);

        lblStatus = new JLabel("Chua tai danh sach");
        lblStatus.setForeground(LanMonitorTheme.BLUE_BTN);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(LanMonitorTheme.DARK_SIDEBAR);
        lblStatus.setFont(LanMonitorTheme.FONT_SMALL);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(lblStatus, BorderLayout.SOUTH);

        btnCreate.addActionListener(e -> openQuizDialog(null));
        btnEdit.addActionListener(e -> {
            int row = quizTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Chon 1 de thi de sua!");
                return;
            }
            if (quizzesData == null || row >= quizzesData.size()) return;
            openQuizDialog(quizzesData.get(row));
        });
        btnDelete.addActionListener(e -> deleteSelectedQuiz());
        btnRefresh.addActionListener(e -> tcpClient.sendCommand("REQ_QUIZ_ALL"));

        dispatcher.addHandler(this::handleServerMessage);

        tcpClient.sendCommand("REQ_QUIZ_ALL");
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("RES_QUIZ_ALL:")) {
            String json = message.substring("RES_QUIZ_ALL:".length());
            SwingUtilities.invokeLater(() -> updateQuizTable(json));
        } else if (message.startsWith("SAVE_QUIZ_OK")) {
            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("Da luu de thi thanh cong");
                tcpClient.sendCommand("REQ_QUIZ_ALL");
            });
        } else if (message.startsWith("SAVE_QUIZ_FAIL")) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Loi luu: " + message.substring(14)));
        } else if (message.startsWith("DELETE_QUIZ_OK")) {
            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("Da xoa de thi");
                tcpClient.sendCommand("REQ_QUIZ_ALL");
            });
        } else if (message.startsWith("DELETE_QUIZ_FAIL")) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Loi xoa: " + message.substring(16)));
        }
    }

    @SuppressWarnings("unchecked")
    private void updateQuizTable(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            quizzesData = mapper.readValue(json, List.class);
            tableModel.setRowCount(0);
            for (Map<String, Object> quiz : quizzesData) {
                String id = String.valueOf(quiz.get("id"));
                String name = String.valueOf(quiz.get("name"));
                Object tlObj = quiz.get("timeLimit");
                int timeLimit = tlObj instanceof Number ? ((Number) tlObj).intValue() : 0;
                List<?> data = (List<?>) quiz.get("data");
                int soCau = data != null ? data.size() : 0;
                Boolean valid = (Boolean) quiz.get("isValid");
                String status = Boolean.TRUE.equals(valid) ? "[DUNG]" : "[SAI]";
                tableModel.addRow(new Object[]{id, name, timeLimit, soCau, status});
            }
            lblStatus.setText("Da tai " + quizzesData.size() + " de thi");
        } catch (Exception e) {
            lblStatus.setText("Loi parse: " + e.getMessage());
        }
    }

    private void deleteSelectedQuiz() {
        int row = quizTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chon 1 de thi de xoa!");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int c = JOptionPane.showConfirmDialog(this,
                "Xoa de thi \"" + name + "\"?",
                "Xac nhan xoa", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            tcpClient.sendCommand("DELETE_QUIZ:" + id);
        }
    }

    private void openQuizDialog(Map<String, Object> existingQuiz) {
        QuizEditDialog dialog = new QuizEditDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                tcpClient, existingQuiz);
        dialog.setVisible(true);
    }
}
