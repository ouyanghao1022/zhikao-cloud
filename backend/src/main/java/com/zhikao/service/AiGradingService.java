package com.zhikao.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * AI辅助批改服务（基于文本相似度的简单NLP评分）
 */
@Service
public class AiGradingService {

    /**
     * 对主观题进行AI预评分
     * @param studentAnswer 学生答案
     * @param referenceAnswer 参考答案
     * @param maxScore 满分
     * @return {score, feedback}
     */
    public Map<String, Object> gradeSubjective(String studentAnswer, String referenceAnswer, int maxScore) {
        Map<String, Object> result = new HashMap<>();

        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            result.put("score", 0);
            result.put("feedback", "未作答");
            return result;
        }
        if (referenceAnswer == null || referenceAnswer.trim().isEmpty()) {
            result.put("score", maxScore);
            result.put("feedback", "无参考答案，请人工批阅");
            return result;
        }

        double similarity = calculateSimilarity(studentAnswer, referenceAnswer);
        int score = (int) Math.round(similarity * maxScore);

        String feedback;
        if (similarity >= 0.8) feedback = "回答优秀，与参考答案高度一致";
        else if (similarity >= 0.6) feedback = "回答良好，基本涵盖要点";
        else if (similarity >= 0.4) feedback = "回答一般，部分要点缺失";
        else if (similarity >= 0.2) feedback = "回答较差，建议参考标准答案";
        else feedback = "回答与参考答案偏差较大";

        result.put("score", score);
        result.put("similarity", Math.round(similarity * 100) / 100.0);
        result.put("feedback", feedback);
        return result;
    }

    /**
     * 计算文本相似度（基于关键词匹配 + 长度比例）
     */
    private double calculateSimilarity(String text1, String text2) {
        String t1 = preprocess(text1);
        String t2 = preprocess(text2);

        Set<String> words1 = new HashSet<>(Arrays.asList(t1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(t2.split("\\s+")));

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        double jaccard = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
        double lenRatio = Math.min(text1.length(), text2.length()) * 1.0 / Math.max(text1.length(), text2.length());

        return jaccard * 0.7 + lenRatio * 0.3;
    }

    private String preprocess(String text) {
        return text.replaceAll("[\\pP\\p{Punct}]", " ")
                   .replaceAll("\\s+", " ")
                   .trim()
                   .toLowerCase();
    }
}
