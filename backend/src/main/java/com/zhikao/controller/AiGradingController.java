package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.service.AiGradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiGradingController {

    @Autowired
    private AiGradingService aiGradingService;

    @PostMapping("/grade")
    public Result<Map<String, Object>> gradeAnswer(@RequestBody Map<String, Object> body) {
        String studentAnswer = (String) body.get("studentAnswer");
        String referenceAnswer = (String) body.get("referenceAnswer");
        int maxScore = body.get("maxScore") != null ? ((Number) body.get("maxScore")).intValue() : 10;

        return Result.success(aiGradingService.gradeSubjective(studentAnswer, referenceAnswer, maxScore));
    }
}
