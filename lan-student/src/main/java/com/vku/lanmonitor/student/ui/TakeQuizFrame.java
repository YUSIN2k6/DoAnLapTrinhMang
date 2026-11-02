package com.vku.lanmonitor.student.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vku.lanmonitor.student.net.StudentTcpClient;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class TakeQuizFrame extends JFrame {
    // Fields
    private final String quizId;
    private final String studentId;
    private final StudentTcpClient tcpClient;
    private boolean submitted = false;

    // Quiz data (parse từ server)
    private Map<String, Object> quizData;
    private List<Map<String, Object>> questions;
    private List<String> userAnswers; // "A"/"B"/"C"/"D"/null
    private int currentIndex = 0;
    private long startTime;
    private int timeLimitSeconds;

    // UI components
    private JLabel lblTitle;
    private JLabel lblTimer;
    private JLabel lblQuestionNumber;
    private JTextArea txtQuestion;
    private JPanel optionsPanel;
    private ButtonGroup optionsGroup;
    private JPanel navigationGrid;
    private JLabel lblStatus;
    private JButton btnPrev, btnNext, btnSubmit;
    private javax.swing.Timer timer;
    private List<JRadioButton> currentOptionButtons = new ArrayList<>();

    public TakeQuizFrame(String quizId, String studentId, StudentTcpClient sharedClient) {
        super("Làm bài thi - LAN Monitor");
        System.out.println("[TQ] === TakeQuizFrame constructor START ===");
        System.out.println("[TQ] quizId=" + quizId + " studentId=" + studentId);

        this.quizId = quizId;
        this.studentId = studentId;
        this.tcpClient = sharedClient;

        setupFrame();

        // Đổi listener + gửi REQ_QUIZ_DETAIL
        tcpClient.setMessageListener(this::handleServerMessage);
        tcpClient.sendCommand("REQ_QUIZ_DETAIL:" + quizId);
        System.out.println("[TQ] Sent REQ_QUIZ_DETAIL:" + quizId);

        setVisible(true);
        System.out.println("[TQ] === TakeQuizFrame constructor END ===");
    }

    private void setupFrame() {
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Xác nhận khi đóng
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int c = JOptionPane.showConfirmDialog(TakeQuizFrame.this,
                        "Bạn có chắc muốn thoát? Bài làm sẽ không được lưu.",
                        "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (c == JOptionPane.YES_OPTION) {
                    if (timer != null)
                        timer.stop();
                    dispose();
                    // Quay lại StudentHomeFrame với sharedClient
                    SwingUtilities.invokeLater(() -> new StudentHomeFrame(studentId, tcpClient).setVisible(true));
                }
            }
        });

        // Layout chính
        setLayout(new BorderLayout(5, 5));

        // NORTH: title + timer
        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(new Color(220, 220, 220));
        north.setPreferredSize(new Dimension(0, 60));

        lblTitle = new JLabel("Đang tải...", SwingConstants.LEFT);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        lblTimer = new JLabel("00:00", SwingConstants.RIGHT);
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 20));
        lblTimer.setForeground(Color.RED);
        lblTimer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

        north.add(lblTitle, BorderLayout.WEST);
        north.add(lblTimer, BorderLayout.EAST);
        add(north, BorderLayout.NORTH);

        // CENTER: câu hỏi (LEFT) + navigation (RIGHT)
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // LEFT: câu hỏi + options
        JPanel questionPanel = new JPanel();
        questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
        questionPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

        lblQuestionNumber = new JLabel("Câu 1 / N");
        lblQuestionNumber.setFont(new Font("Arial", Font.BOLD, 14));
        lblQuestionNumber.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtQuestion = new JTextArea();
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setEditable(false);
        txtQuestion.setFont(new Font("Arial", Font.PLAIN, 15));
        txtQuestion.setBackground(new Color(245, 245, 245));
        txtQuestion.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane questionScroll = new JScrollPane(txtQuestion);
        questionScroll.setPreferredSize(new Dimension(600, 150));
        questionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.setBackground(Color.WHITE);
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        questionPanel.add(lblQuestionNumber);
        questionPanel.add(Box.createVerticalStrut(10));
        questionPanel.add(questionScroll);
        questionPanel.add(Box.createVerticalStrut(15));
        questionPanel.add(optionsPanel);

        // RIGHT: navigation grid
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BorderLayout());
        navPanel.setBorder(BorderFactory.createTitledBorder("Bảng câu hỏi"));
        navPanel.setPreferredSize(new Dimension(220, 0));

        navigationGrid = new JPanel();
        navigationGrid.setLayout(new GridLayout(0, 5, 5, 5));
        JScrollPane navScroll = new JScrollPane(navigationGrid);
        navPanel.add(navScroll, BorderLayout.CENTER);

        centerPanel.add(questionPanel, BorderLayout.CENTER);
        centerPanel.add(navPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        // SOUTH: navigation buttons + status
        JPanel southOuter = new JPanel(new BorderLayout());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPrev = new JButton("← Câu trước");
        btnNext = new JButton("Câu sau →");
        btnSubmit = new JButton("Nộp bài");
        btnSubmit.setBackground(new Color(50, 150, 50));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setOpaque(true);
        btnSubmit.setBorderPainted(false);

        btnPrev.addActionListener(e -> showQuestion(currentIndex - 1));
        btnNext.addActionListener(e -> showQuestion(currentIndex + 1));
        btnSubmit.addActionListener(e -> confirmSubmit());

        south.add(btnPrev);
        south.add(btnNext);
        south.add(btnSubmit);

        lblStatus = new JLabel("Đang tải đề thi...", SwingConstants.CENTER);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(255, 255, 200));
        lblStatus.setPreferredSize(new Dimension(0, 30));

        southOuter.add(south, BorderLayout.CENTER);
        southOuter.add(lblStatus, BorderLayout.SOUTH);
        add(southOuter, BorderLayout.SOUTH);
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("RES_QUIZ_DETAIL:")) {
            String json = message.substring("RES_QUIZ_DETAIL:".length());
            SwingUtilities.invokeLater(() -> loadQuizData(json));
        } else if (message.startsWith("SUBMIT_OK")) {
            SwingUtilities.invokeLater(() -> {
                if (timer != null) {
                    timer.stop();
                    timer = null;
                }
                long timeSpent = (System.currentTimeMillis() - startTime) / 1000;
                String quizName = quizData != null ? String.valueOf(quizData.get("name")) : "Bài thi";

                this.dispose();

                ResultFrame rf = new ResultFrame(
                        studentId, tcpClient, quizName,
                        questions, userAnswers, timeSpent);
                rf.setVisible(true);
            });
        } else if (message.startsWith("SUBMIT_FAIL")) {
            SwingUtilities.invokeLater(() -> lblStatus.setText("Lỗi nộp bài: " + message.substring(11)));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadQuizData(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            quizData = mapper.readValue(json, Map.class);

            questions = (List<Map<String, Object>>) quizData.get("data");
            if (questions == null || questions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Đề thi không có câu hỏi!");
                dispose();
                return;
            }

            // Xáo trộn nếu cần
            Boolean isRandom = (Boolean) quizData.get("isRandom");
            if (Boolean.TRUE.equals(isRandom)) {
                Collections.shuffle(questions);
            }

            int n = questions.size();
            userAnswers = new ArrayList<>(Collections.nCopies(n, null));

            // Timer
            Object tlObj = quizData.get("timeLimit");
            int timeLimit = tlObj instanceof Number ? ((Number) tlObj).intValue() : 15;
            timeLimitSeconds = timeLimit * 60;

            // Title
            String quizName = String.valueOf(quizData.get("name"));
            lblTitle.setText("Bài thi: " + quizName);

            // Build navigation grid
            buildNavigationGrid(n);

            // Show first question
            currentIndex = 0;
            showQuestion(0);

            // Start timer
            startTime = System.currentTimeMillis();
            startTimer();

            lblStatus.setText("Đang làm bài. Thời gian: " + timeLimit + " phút");
            System.out.println("[DEBUG] Loaded quiz with " + n + " questions, timeLimit=" + timeLimit);

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Lỗi parse: " + e.getMessage());
        }
    }

    private void buildNavigationGrid(int n) {
        navigationGrid.removeAll();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            JButton btn = new JButton(String.valueOf(i + 1));
            btn.setMargin(new Insets(2, 2, 2, 2));
            btn.setFont(new Font("Arial", Font.PLAIN, 11));
            btn.addActionListener(e -> showQuestion(idx));
            navigationGrid.add(btn);
        }
        navigationGrid.revalidate();
        navigationGrid.repaint();
    }

    @SuppressWarnings("unchecked")
    private void showQuestion(int index) {
        if (index < 0 || index >= questions.size())
            return;
        currentIndex = index;
        Map<String, Object> q = questions.get(index);

        // Header
        lblQuestionNumber.setText("Câu " + (index + 1) + " / " + questions.size());

        // Question text
        txtQuestion.setText(String.valueOf(q.get("question")));
        txtQuestion.setCaretPosition(0);

        // Options
        optionsPanel.removeAll();
        currentOptionButtons.clear();

        Object optObj = q.get("options");
        if (optObj instanceof Map) {
            Map<String, String> options = (Map<String, String>) optObj;
            optionsGroup = new ButtonGroup();

            List<String> keys = new ArrayList<>(options.keySet());
            Collections.sort(keys); // A, B, C, D

            for (String key : keys) {
                String optText = options.get(key);
                JRadioButton rb = new JRadioButton(key + ". " + optText);
                rb.setFont(new Font("Arial", Font.PLAIN, 14));
                rb.setBackground(Color.WHITE);
                rb.setOpaque(true);
                rb.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                rb.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Check nếu đã chọn
                if (key.equals(userAnswers.get(index))) {
                    rb.setSelected(true);
                }

                final String k = key;
                rb.addActionListener(e -> {
                    userAnswers.set(index, k);
                    updateNavButton(index);
                });

                optionsGroup.add(rb);
                currentOptionButtons.add(rb);
                optionsPanel.add(rb);
                optionsPanel.add(Box.createVerticalStrut(5));
            }
        }

        // Update nav button highlight
        updateNavButton(index);

        // Enable/disable prev/next
        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < questions.size() - 1);

        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    private void updateNavButton(int index) {
        if (index < 0 || index >= navigationGrid.getComponentCount())
            return;
        JButton btn = (JButton) navigationGrid.getComponent(index);
        if (userAnswers.get(index) != null) {
            btn.setBackground(new Color(150, 220, 150)); // Xanh = đã trả lời
            btn.setOpaque(true);
            btn.setBorderPainted(false);
        } else {
            btn.setBackground(null);
            btn.setOpaque(false);
            btn.setBorderPainted(true);
        }
        // Highlight câu hiện tại
        for (int i = 0; i < navigationGrid.getComponentCount(); i++) {
            JButton b = (JButton) navigationGrid.getComponent(i);
            if (i == currentIndex) {
                b.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            } else {
                b.setBorder(UIManager.getBorder("Button.border"));
            }
        }
    }

    private void startTimer() {
        timer = new javax.swing.Timer(1000, e -> {
            if (submitted)
                return;
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            int remaining = timeLimitSeconds - (int) elapsed;

            if (remaining <= 0) {
                if (timer != null)
                    timer.stop();
                lblTimer.setText("00:00");
                JOptionPane.showMessageDialog(this,
                        "Hết thời gian! Hệ thống tự động nộp bài.",
                        "Hết giờ", JOptionPane.WARNING_MESSAGE);
                if (!submitted) {
                    submitQuiz();
                }
                return;
            }

            int min = remaining / 60;
            int sec = remaining % 60;
            lblTimer.setText(String.format("%02d:%02d", min, sec));

            // Cảnh báo khi còn 5 phút
            if (remaining == 300) {
                lblTimer.setForeground(Color.ORANGE);
                JOptionPane.showMessageDialog(this, "Còn 5 phút!", "Cảnh báo",
                        JOptionPane.WARNING_MESSAGE);
            }
            if (remaining <= 60) {
                lblTimer.setForeground(Color.RED);
            }
        });
        timer.start();
    }

    private void confirmSubmit() {
        if (submitted)
            return;

        int unanswered = 0;
        for (String ans : userAnswers) {
            if (ans == null)
                unanswered++;
        }

        String msg = "Bạn có chắc muốn nộp bài?";
        if (unanswered > 0) {
            msg += "\n\nCòn " + unanswered + " câu chưa trả lời!";
        }

        int c = JOptionPane.showConfirmDialog(this, msg, "Xác nhận nộp",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (c == JOptionPane.YES_OPTION) {
            if (timer != null)
                timer.stop();
            submitQuiz();
        }
    }

    private void submitQuiz() {
        if (submitted) {
            System.out.println("[TQ] Already submitted, ignoring");
            return;
        }
        submitted = true;
        
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> submission = new HashMap<>();
            submission.put("studentId", studentId);
            submission.put("quizId", quizId);
            submission.put("quizName", quizData.get("name"));
            submission.put("timeSpent", (System.currentTimeMillis() - startTime) / 1000);
            submission.put("questions", questions);
            submission.put("userAnswers", userAnswers);
            submission.put("images", quizData.get("images"));
            submission.put("submittedAt", System.currentTimeMillis());
            
            String json = mapper.writeValueAsString(submission);
            tcpClient.sendCommand("SUBMIT_QUIZ:" + json);
            lblStatus.setText("Đang nộp bài...");
            System.out.println("[DEBUG] Submitted quiz, json length=" + json.length());
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Lỗi nộp bài: " + e.getMessage());
        }
    }
}
