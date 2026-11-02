package com.vku.lanmonitor.admin.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vku.lanmonitor.admin.net.AdminTcpClient;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizEditDialog extends JDialog {
    private final AdminTcpClient tcpClient;
    private final Map<String, Object> existingQuiz;
    private final JTextField txtName;
    private final JTextField txtTimeLimit;
    private final JTextArea txtJson;
    private final JCheckBox chkRandom;
    private final JLabel lblStatus;

    public QuizEditDialog(JFrame parent, AdminTcpClient tcpClient, Map<String, Object> existingQuiz) {
        super(parent, existingQuiz == null ? "Tạo đề mới" : "Sửa đề thi", true);
        this.tcpClient = tcpClient;
        this.existingQuiz = existingQuiz;

        setSize(800, 700);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel pName = new JPanel(new BorderLayout(5, 5));
        pName.add(new JLabel("Tên đề thi:"), BorderLayout.NORTH);
        txtName = new JTextField(existingQuiz != null ? String.valueOf(existingQuiz.get("name")) : "");
        txtName.setFont(new Font("Arial", Font.PLAIN, 13));
        pName.add(txtName, BorderLayout.CENTER);
        mainPanel.add(pName);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel pTime = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pTime.add(new JLabel("Thời gian (phút):"));
        txtTimeLimit = new JTextField(5);
        if (existingQuiz != null && existingQuiz.get("timeLimit") instanceof Number) {
            txtTimeLimit.setText(String.valueOf(((Number) existingQuiz.get("timeLimit")).intValue()));
        } else {
            txtTimeLimit.setText("15");
        }
        pTime.add(txtTimeLimit);
        pTime.add(Box.createHorizontalStrut(20));
        chkRandom = new JCheckBox("Xáo trộn câu hỏi");
        if (existingQuiz != null && Boolean.TRUE.equals(existingQuiz.get("isRandom"))) {
            chkRandom.setSelected(true);
        }
        pTime.add(chkRandom);
        pTime.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(pTime);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel pJson = new JPanel(new BorderLayout(5, 5));
        pJson.add(new JLabel("Nội dung câu hỏi (JSON array):"), BorderLayout.NORTH);
        txtJson = new JTextArea();
        txtJson.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtJson.setLineWrap(false);
        if (existingQuiz != null && existingQuiz.get("data") != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                txtJson.setText(mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(existingQuiz.get("data")));
            } catch (Exception e) {
                txtJson.setText("");
            }
        } else {
            txtJson.setText("[\n  {\n    \"question\": \"Câu hỏi mẫu?\",\n    \"options\": {\"A\": \"Đáp án A\", \"B\": \"Đáp án B\", \"C\": \"Đáp án C\", \"D\": \"Đáp án D\"},\n    \"correctAnswer\": \"A\",\n    \"explanation\": \"Giải thích\"\n  }\n]");
        }
        JScrollPane scroll = new JScrollPane(txtJson);
        scroll.setPreferredSize(new Dimension(750, 400));
        pJson.add(scroll, BorderLayout.CENTER);
        pJson.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(pJson);

        add(mainPanel, BorderLayout.CENTER);

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 20, 0, 20));
        add(lblStatus, BorderLayout.NORTH);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnSave = new JButton("Lưu");
        btnSave.setBackground(new Color(50, 150, 50));
        btnSave.setForeground(Color.WHITE);
        btnSave.setOpaque(true);
        btnSave.setBorderPainted(false);
        JButton btnCancel = new JButton("Hủy");

        btnSave.addActionListener(e -> handleSave());
        btnCancel.addActionListener(e -> dispose());

        south.add(btnSave);
        south.add(btnCancel);
        add(south, BorderLayout.SOUTH);
    }

    private void handleSave() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            lblStatus.setText("Vui lòng nhập tên đề thi!");
            return;
        }
        int timeLimit;
        try {
            timeLimit = Integer.parseInt(txtTimeLimit.getText().trim());
        } catch (Exception e) {
            lblStatus.setText("Thời gian phải là số nguyên!");
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object data = mapper.readValue(txtJson.getText(), Object.class);

            Map<String, Object> quiz = new HashMap<>();
            if (existingQuiz != null) {
                quiz.put("id", existingQuiz.get("id"));
            } else {
                quiz.put("id", System.currentTimeMillis());
            }
            quiz.put("name", name);
            quiz.put("timeLimit", timeLimit);
            quiz.put("isRandom", chkRandom.isSelected());
            quiz.put("data", data);
            quiz.put("isValid", data instanceof List && !((List<?>) data).isEmpty());
            quiz.put("images", existingQuiz != null ? existingQuiz.get("images") : new ArrayList<>());

            String json = mapper.writeValueAsString(quiz);
            tcpClient.sendCommand("SAVE_QUIZ:" + json);
            dispose();
        } catch (Exception e) {
            lblStatus.setText("JSON không hợp lệ: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
