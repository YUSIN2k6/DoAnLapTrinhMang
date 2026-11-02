package com.vku.lanmonitor.student.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vku.lanmonitor.student.net.StudentTcpClient;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

public class StudentHomeFrame extends JFrame {
    private final String studentId;
    private final StudentTcpClient tcpClient;
    private final DefaultListModel<String> listModel;
    private final JList<String> quizList;
    private final JLabel lblStatus;
    private List<Map<String, Object>> quizzesData;

    public StudentHomeFrame(String studentId, StudentTcpClient sharedClient) {
        super("Phòng chờ - LAN Monitor");
        this.studentId = studentId;
        this.tcpClient = sharedClient;

        // Frame
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int c = JOptionPane.showConfirmDialog(StudentHomeFrame.this,
                    "Bạn có chắc muốn thoát ứng dụng?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (c == JOptionPane.YES_OPTION) {
                    handleExit();
                }
            }
        });

        // NORTH — header
        JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        north.setBackground(new Color(220, 220, 220));
        JLabel lblWelcome = new JLabel("Xin chào: " + studentId);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 16));
        north.add(lblWelcome);

        // CENTER — JList
        listModel = new DefaultListModel<>();
        listModel.addElement("ĐANG TẢI DANH SÁCH BÀI THI...");
        quizList = new JList<>(listModel);
        quizList.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(quizList);

        // SOUTH — buttons + status
        JPanel southOuter = new JPanel(new BorderLayout());

        JPanel southButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnStart = new JButton("Bắt đầu làm bài");
        btnStart.setFont(new Font("Arial", Font.BOLD, 13));
        btnStart.setBackground(new Color(50, 150, 50));
        btnStart.setForeground(Color.WHITE);
        btnStart.setOpaque(true);
        btnStart.setBorderPainted(false);
        
        btnStart.addActionListener(e -> {
            int idx = quizList.getSelectedIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 bài thi!");
                return;
            }
            if (quizzesData == null || idx >= quizzesData.size()) {
                JOptionPane.showMessageDialog(this, "Lỗi dữ liệu quiz!");
                return;
            }
            Map<String, Object> quiz = quizzesData.get(idx);
            String quizId = String.valueOf(quiz.get("id"));
            
            this.setVisible(false);
            TakeQuizFrame tq = new TakeQuizFrame(quizId, studentId, tcpClient);
            tq.setVisible(true);
        });

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Arial", Font.PLAIN, 13));
        btnLogout.addActionListener(e -> handleLogout());

        southButtons.add(btnStart);
        southButtons.add(btnLogout);

        lblStatus = new JLabel("Sẵn sàng", SwingConstants.CENTER);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(255, 255, 200));
        lblStatus.setPreferredSize(new Dimension(0, 30));

        southOuter.add(southButtons, BorderLayout.CENTER);
        southOuter.add(lblStatus, BorderLayout.SOUTH);

        // Layout
        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(southOuter, BorderLayout.SOUTH);

        // Đổi listener + gửi REQ_QUIZ_LIST
        tcpClient.setMessageListener(this::handleServerMessage);
        tcpClient.sendCommand("REQ_QUIZ_LIST");

        setVisible(true);
    }

    private void handleServerMessage(String message) {
        System.out.println("[DEBUG] handleServerMessage: " + 
            message.substring(0, Math.min(60, message.length())));
        if (message.startsWith("RES_QUIZ_LIST:")) {
            String json = message.substring("RES_QUIZ_LIST:".length());
            System.out.println("[DEBUG] Got RES_QUIZ_LIST, json len: " + json.length());
            SwingUtilities.invokeLater(() -> updateQuizList(json));
        } else {
            System.out.println("[DEBUG] Ignored (unknown prefix)");
        }
    }

    @SuppressWarnings("unchecked")
    private void updateQuizList(String json) {
        System.out.println("[DEBUG] === updateQuizList START ===");
        System.out.println("[DEBUG] json length: " + json.length());
        try {
            ObjectMapper mapper = new ObjectMapper();
            quizzesData = mapper.readValue(json, List.class);
            System.out.println("[DEBUG] parsed " + quizzesData.size());

            // Xóa "ĐANG TẢI" cũ
            listModel.clear();

            if (quizzesData.isEmpty()) {
                listModel.addElement("Chưa có bài thi nào");
            } else {
                for (int i = 0; i < quizzesData.size(); i++) {
                    Map<String, Object> quiz = quizzesData.get(i);
                    String name = String.valueOf(quiz.get("name"));
                    Object tl = quiz.get("timeLimit");
                    List<?> data = (List<?>) quiz.get("data");
                    int soCau = data != null ? data.size() : 0;
                    listModel.addElement(String.format("[%d] %s — %s phút — %d câu",
                            i + 1, name, tl, soCau));
                }
            }

            setTitle("Phòng chờ - LAN Monitor (" + quizzesData.size() + " bài thi)");
            lblStatus.setText("Đã tải " + quizzesData.size() + " bài thi");
            System.out.println("[DEBUG] updateQuizList DONE");
        } catch (Exception e) {
            e.printStackTrace();
            listModel.clear();
            listModel.addElement("LỖI: " + e.getMessage());
            lblStatus.setText("Lỗi parse");
        }
        System.out.println("[DEBUG] === updateQuizList END ===");
    }

    private void handleLogout() {
        new Thread(() -> {
            try {
                if (tcpClient != null) {
                    tcpClient.disconnect();
                }
            } catch (Exception ignored) {
            }
        }).start();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private void handleExit() {
        dispose();
        new Thread(() -> {
            try {
                if (tcpClient != null) {
                    tcpClient.disconnect();
                }
            } catch (Exception ignored) {
            }
            System.exit(0);
        }).start();
    }
}
