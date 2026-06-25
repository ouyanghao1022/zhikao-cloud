package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.service.SmartRecommendService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart")
public class SmartRecommendController {

    @Autowired
    private SmartRecommendService smartRecommendService;

    @GetMapping("/review")
    public Result<List<Map<String, Object>>> todayReview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(smartRecommendService.getTodayRecommend(userId));
    }

    @GetMapping("/similar/{questionId}")
    public Result<?> similarQuestions(@PathVariable Long questionId) {
        return Result.success(smartRecommendService.getSimilarQuestions(questionId));
    }
}
