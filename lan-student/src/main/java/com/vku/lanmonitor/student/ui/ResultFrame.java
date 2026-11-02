package com.vku.lanmonitor.student.ui;

import com.vku.lanmonitor.student.net.StudentTcpClient;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

public class ResultFrame extends JFrame {
    private final String studentId;
    private final StudentTcpClient tcpClient;
    private final String quizName;
    private final List<Map<String, Object>> questions;
    private final List<String> userAnswers;
    private final long timeSpentSeconds;

    private static final Color COLOR_GREEN = new Color(0, 150, 0);
    private static final Color COLOR_RED = new Color(200, 0, 0);
    private static final Color COLOR_GREY = Color.DARK_GRAY;

    public ResultFrame(String studentId, StudentTcpClient tcpClient,
                       String quizName, List<Map<String, Object>> questions,
                       List<String> userAnswers, long timeSpentSeconds) {
        super("Kết quả bài thi - LAN Monitor");
        this.studentId = studentId;
        this.tcpClient = tcpClient;
        this.quizName = quizName;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.userAnswers = userAnswers != null ? userAnswers : new ArrayList<>();
        this.timeSpentSeconds = timeSpentSeconds;

        initComponents();
    }

    private void initComponents() {
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                goBackToHome();
            }
        });

        setLayout(new BorderLayout(5, 5));

        // NORTH: Header
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        northPanel.setBackground(new Color(220, 220, 220));
        JLabel lblHeader = new JLabel("Kết quả: " + quizName);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 18));
        northPanel.add(lblHeader);
        add(northPanel, BorderLayout.NORTH);

        // Calculate score
        int totalQuestions = questions.size();
        int correctCount = 0;
        for (int i = 0; i < totalQuestions; i++) {
            String correct = String.valueOf(questions.get(i).get("correctAnswer"));
            String userAns = i < userAnswers.size() ? userAnswers.get(i) : null;
            if (correct.equalsIgnoreCase(userAns)) {
                correctCount++;
            }
        }
        double score = totalQuestions > 0 ? (correctCount * 10.0) / totalQuestions : 0.0;
        String scoreText = String.format("%.2f", score);

        long min = timeSpentSeconds / 60;
        long sec = timeSpentSeconds % 60;
        String timeText = min + " phút " + sec + " giây";

        // CENTER: Summary panel + Details scroll pane
        JPanel centerContainer = new JPanel(new BorderLayout(5, 10));
        centerContainer.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Summary panel
        JPanel summaryPanel = new JPanel(new GridLayout(2, 2, 15, 10));
        summaryPanel.setBackground(new Color(245, 248, 250));
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblQuizTitle = new JLabel("Tên bài: " + quizName);
        lblQuizTitle.setFont(new Font("Arial", Font.PLAIN, 15));

        JLabel lblScore = new JLabel("Điểm: " + scoreText + " / 10");
        lblScore.setFont(new Font("Arial", Font.BOLD, 22));
        lblScore.setForeground(score >= 5.0 ? COLOR_GREEN : COLOR_RED);

        JLabel lblCorrect = new JLabel("Số câu đúng: " + correctCount + " / " + totalQuestions);
        lblCorrect.setFont(new Font("Arial", Font.PLAIN, 15));

        JLabel lblTime = new JLabel("Thời gian làm bài: " + timeText);
        lblTime.setFont(new Font("Arial", Font.PLAIN, 15));

        summaryPanel.add(lblQuizTitle);
        summaryPanel.add(lblScore);
        summaryPanel.add(lblCorrect);
        summaryPanel.add(lblTime);

        centerContainer.add(summaryPanel, BorderLayout.NORTH);

        // Details list (question cards)
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < totalQuestions; i++) {
            Map<String, Object> q = questions.get(i);
            String userAns = i < userAnswers.size() ? userAnswers.get(i) : null;
            JPanel card = createQuestionResultCard(i + 1, q, userAns);
            detailsPanel.add(card);
            detailsPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chi tiết bài làm"));
        centerContainer.add(scrollPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);

        // SOUTH: Buttons
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        JButton btnBack = new JButton("Quay lại danh sách bài thi");
        btnBack.setFont(new Font("Arial", Font.BOLD, 14));
        btnBack.setBackground(new Color(60, 130, 200));
        btnBack.setForeground(Color.WHITE);
        btnBack.setOpaque(true);
        btnBack.setBorderPainted(false);
        btnBack.addActionListener(e -> goBackToHome());

        southPanel.add(btnBack);
        add(southPanel, BorderLayout.SOUTH);
    }

    @SuppressWarnings("unchecked")
    private JPanel createQuestionResultCard(int qIndex, Map<String, Object> q, String userAns) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(252, 252, 254));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 215, 220), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        String questionText = String.valueOf(q.get("question"));
        String correctAnswer = String.valueOf(q.get("correctAnswer"));

        boolean isCorrect = correctAnswer.equalsIgnoreCase(userAns);

        // Question title
        JLabel lblQ = new JLabel(String.format("Câu %d: %s", qIndex, questionText));
        lblQ.setFont(new Font("Arial", Font.BOLD, 14));
        lblQ.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblQ);
        card.add(Box.createVerticalStrut(8));

        // Options
        Object optObj = q.get("options");
        if (optObj instanceof Map) {
            Map<String, String> options = (Map<String, String>) optObj;
            List<String> keys = new ArrayList<>(options.keySet());
            Collections.sort(keys);

            for (String key : keys) {
                String optText = options.get(key);
                JLabel lblOpt;
                if (key.equalsIgnoreCase(correctAnswer)) {
                    lblOpt = new JLabel("[ĐÚNG] " + key + ". " + optText);
                    lblOpt.setForeground(COLOR_GREEN);
                    lblOpt.setFont(new Font("Arial", Font.BOLD, 13));
                } else if (key.equalsIgnoreCase(userAns)) {
                    lblOpt = new JLabel("[SAI] " + key + ". " + optText);
                    lblOpt.setForeground(COLOR_RED);
                    lblOpt.setFont(new Font("Arial", Font.BOLD, 13));
                } else {
                    lblOpt = new JLabel("          " + key + ". " + optText);
                    lblOpt.setForeground(COLOR_GREY);
                    lblOpt.setFont(new Font("Arial", Font.PLAIN, 13));
                }
                lblOpt.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(lblOpt);
                card.add(Box.createVerticalStrut(3));
            }
        }

        card.add(Box.createVerticalStrut(6));

        // User answer summary label
        String userChoiceText = userAns != null ? userAns : "Chưa trả lời";
        JLabel lblUser = new JLabel("Bạn chọn: " + userChoiceText + (isCorrect ? " (Đúng)" : " (Sai)"));
        lblUser.setFont(new Font("Arial", Font.ITALIC, 13));
        lblUser.setForeground(isCorrect ? COLOR_GREEN : COLOR_RED);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblUser);

        return card;
    }

    private void goBackToHome() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            StudentHomeFrame homeFrame = new StudentHomeFrame(studentId, tcpClient);
            homeFrame.setVisible(true);
        });
    }
}
