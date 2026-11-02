package com.vku.lanmonitor.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class QuizService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String DIR = "storage";
    private final String QUIZ_FILE = DIR + "/quizzes.json";

    public QuizService() {
        try {
            Files.createDirectories(Paths.get(DIR));
            Files.createDirectories(Paths.get(DIR + "/quiz_images"));
            Files.createDirectories(Paths.get(DIR + "/results"));
        } catch (Exception e) {
            log.error("Không thể tạo thư mục storage: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Object> loadAllQuizzes() {
        try {
            File file = new File(QUIZ_FILE);
            if (file.exists()) {
                return mapper.readValue(file, List.class);
            }
        } catch (Exception e) {
            log.error("Lỗi lấy đề thi: ", e);
        }
        return new ArrayList<>();
    }

    public void saveAllQuizzes(List<Object> quizzes) throws Exception {
        mapper.writeValue(new File(QUIZ_FILE), quizzes);
    }

    public Object loadQuizById(String quizId) {
        if (quizId == null) {
            return null;
        }
        for (Object quiz : loadAllQuizzes()) {
            if (quiz instanceof Map<?, ?> map && quizId.equals(String.valueOf(map.get("id")))) {
                return quiz;
            }
        }
        return null;
    }

    public void saveQuizResult(Map<String, Object> resultData) throws Exception {
        String studentId = String.valueOf(resultData.getOrDefault("studentId", "UNKNOWN")).replaceAll("[^a-zA-Z0-9.-]", "_");
        String quizId = String.valueOf(resultData.get("quizId")).replaceAll("[^a-zA-Z0-9.-]", "_");
        String fileName = DIR + "/results/" + studentId + "_" + quizId + "_" + System.currentTimeMillis() + ".json";
        mapper.writeValue(new File(fileName), resultData);
        log.info(">>> Đã thu bài của sinh viên [{}] cho mã đề [{}]", studentId, quizId);
    }

    public boolean deleteQuizImage(String fileName) {
        try {
            String sanitizedFileName = new File(fileName).getName();
            File img = new File(DIR + "/quiz_images/" + sanitizedFileName);
            if (img.exists()) {
                return img.delete();
            }
            return true;
        } catch (Exception e) {
            log.error("Lỗi xóa ảnh đề thi: ", e);
            return false;
        }
    }
}
