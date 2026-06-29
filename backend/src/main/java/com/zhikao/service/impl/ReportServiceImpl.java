package com.zhikao.service.impl;

import com.zhikao.mapper.ReportMapper;
import com.zhikao.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 学情报告Service实现类
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Override
    public Map<String, Object> getPersonalReport(Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 能力雷达图数据 - 各学科正确率
        List<Map<String, Object>> categories = reportMapper.selectCategoryPerformance(userId);
        List<String> radarIndicators = new ArrayList<>();
        List<Double> radarValues = new ArrayList<>();
        if (categories != null && !categories.isEmpty()) {
            for (Map<String, Object> cat : categories) {
                radarIndicators.add((String) cat.get("category_name"));
                Object rate = cat.get("correct_rate");
                radarValues.add(rate != null ? ((Number) rate).doubleValue() : 0.0);
            }
        }
        Map<String, Object> radarData = new HashMap<>();
        radarData.put("indicators", radarIndicators);
        radarData.put("values", radarValues);
        result.put("radarData", radarData);

        // 2. 折线图数据 - 考试成绩趋势
        List<Map<String, Object>> trendData = reportMapper.selectExamTrend(userId);
        result.put("trendData", trendData);

        // 3. 总体统计
        int totalExams = trendData != null ? trendData.size() : 0;
        double avgScore = 0;
        if (totalExams > 0) {
            double sum = 0;
            for (Map<String, Object> row : trendData) {
                Object score = row.get("total_score");
                if (score != null) {
                    sum += ((Number) score).doubleValue();
                }
            }
            avgScore = Math.round(sum / totalExams * 100.0) / 100.0;
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalExams", totalExams);
        summary.put("avgScore", avgScore);
        result.put("summary", summary);

        return result;
    }

    @Override
    public Map<String, Object> getExamTrend(Long userId) {
        List<Map<String, Object>> trendData = reportMapper.selectExamTrend(userId);

        List<String> examLabels = new ArrayList<>();
        List<Double> scoreValues = new ArrayList<>();
        List<String> paperTitles = new ArrayList<>();

        if (trendData != null) {
            for (Map<String, Object> row : trendData) {
                Object time = row.get("submit_time");
                examLabels.add(time != null ? time.toString().substring(0, 10) : "");
                Object score = row.get("total_score");
                scoreValues.add(score != null ? ((Number) score).doubleValue() : 0.0);
                Object title = row.get("paper_title");
                paperTitles.add(title != null ? title.toString() : "");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", examLabels);
        result.put("scores", scoreValues);
        result.put("paperTitles", paperTitles);
        result.put("data", trendData);
        return result;
    }

    @Override
    public Map<String, Object> getKnowledgeHeatmap(Long userId) {
        List<Map<String, Object>> heatmapData = reportMapper.selectKnowledgeHeatmap(userId);

        Map<String, Object> result = new HashMap<>();
        // 构造热力图数据：每个知识点一行，包含 知识点名称, 所属分类, 正确率
        List<Map<String, Object>> heatmapItems = new ArrayList<>();
        if (heatmapData != null) {
            for (Map<String, Object> row : heatmapData) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", row.get("knowledge_name"));
                item.put("rate", row.get("correct_rate"));
                item.put("total", row.get("total_count"));
                item.put("correct", row.get("correct_count"));
                heatmapItems.add(item);
            }
        }
        result.put("items", heatmapItems);
        result.put("maxRate", 100);
        return result;
    }

    @Override
    public Map<String, Object> getWeakAreas(Long userId) {
        List<Map<String, Object>> weakData = reportMapper.selectWeakAreas(userId);

        Map<String, Object> result = new HashMap<>();
        // 去重汇总：按知识点合并
        Map<String, Map<String, Object>> knowledgeMap = new LinkedHashMap<>();
        if (weakData != null) {
            for (Map<String, Object> row : weakData) {
                String name = (String) row.get("knowledge_name");
                if (name == null) continue;

                knowledgeMap.computeIfAbsent(name, k -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("knowledgeName", name);
                    entry.put("wrongCount", 0);
                    entry.put("correctRate", 0.0);
                    List<String> errorTypes = new ArrayList<>();
                    entry.put("errorTypes", errorTypes);
                    return entry;
                });

                Map<String, Object> entry = knowledgeMap.get(name);
                Object wc = row.get("wrong_count");
                entry.put("wrongCount", ((Number) entry.get("wrongCount")).intValue()
                        + (wc != null ? ((Number) wc).intValue() : 0));

                Object rate = row.get("correct_rate");
                if (rate != null && ((Number) rate).doubleValue() < ((Number) entry.get("correctRate")).doubleValue()
                        || ((Number) entry.get("correctRate")).doubleValue() == 0) {
                    entry.put("correctRate", ((Number) rate).doubleValue());
                }

                Object errorType = row.get("error_type");
                if (errorType != null) {
                    String typeDesc = mapErrorType(((Number) errorType).intValue());
                    @SuppressWarnings("unchecked")
                    List<String> types = (List<String>) entry.get("errorTypes");
                    if (!types.contains(typeDesc)) {
                        types.add(typeDesc);
                    }
                }
            }
        }

        List<Map<String, Object>> weakList = new ArrayList<>(knowledgeMap.values());
        result.put("items", weakList);

        // 错误类型统计
        List<Map<String, Object>> errorTypeStats = new ArrayList<>();
        Map<Integer, Integer> typeCounts = new HashMap<>();
        if (weakData != null) {
            for (Map<String, Object> row : weakData) {
                Object et = row.get("error_type");
                if (et != null) {
                    int type = ((Number) et).intValue();
                    Object wc = row.get("wrong_count");
                    typeCounts.merge(type, wc != null ? ((Number) wc).intValue() : 0, Integer::sum);
                }
            }
        }
        for (Map.Entry<Integer, Integer> e : typeCounts.entrySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("errorType", e.getKey());
            stat.put("errorName", mapErrorType(e.getKey()));
            stat.put("count", e.getValue());
            errorTypeStats.add(stat);
        }
        result.put("errorTypeStats", errorTypeStats);

        return result;
    }

    @Override
    public Map<String, Object> getClassReport(Long paperId, Long classId) {
        Map<String, Object> result = new HashMap<>();

        // 班级基本统计（仅统计指定班级的学生）
        Map<String, Object> stats = reportMapper.selectClassStats(paperId, classId);
        result.put("stats", stats != null ? stats : new HashMap<>());

        // 分数段分布
        List<Map<String, Object>> distribution = reportMapper.selectClassScoreDistribution(paperId, classId);
        result.put("distribution", distribution != null ? distribution : new ArrayList<>());

        // 考生明细（含 sessionId、是否已颁证）— 给教师/管理员手动颁发证书用
        List<Map<String, Object>> students = reportMapper.selectClassStudentList(paperId, classId);
        result.put("students", students != null ? students : new ArrayList<>());

        return result;
    }

    private String mapErrorType(int type) {
        switch (type) {
            case 1: return "粗心大意";
            case 2: return "完全不会";
            case 3: return "审题错误";
            default: return "概念不清";
        }
    }
}
