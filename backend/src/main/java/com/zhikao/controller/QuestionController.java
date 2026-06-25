package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.PageRequest;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.Question;
import com.zhikao.entity.QuestionAnalysis;
import com.zhikao.entity.QuestionCategory;
import com.zhikao.entity.QuestionFavorite;
import com.zhikao.entity.QuestionOption;
import com.zhikao.entity.QuestionTag;
import com.zhikao.mapper.QuestionCategoryMapper;
import com.zhikao.mapper.QuestionOptionMapper;
import com.zhikao.service.QuestionAnalysisService;
import com.zhikao.service.QuestionFavoriteService;
import com.zhikao.service.QuestionService;
import com.zhikao.service.QuestionTagService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题库管理控制器
 */
@RestController
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionCategoryMapper categoryMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    @Autowired
    private QuestionTagService questionTagService;

    @Autowired
    private QuestionAnalysisService questionAnalysisService;

    @Autowired
    private QuestionFavoriteService questionFavoriteService;

    /**
     * 分页查询题目列表
     * TEACHER 只能看到自己创建的题目，SUPER_ADMIN 可看到全部
     */
    @GetMapping("/list")
    public Result<PageResult<Question>> list(PageRequest pageRequest,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) Integer questionType,
                                               @RequestParam(required = false) Integer difficulty,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) List<Long> creatorIds,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 教师强制限定为自己的题目（忽略传入 creatorIds）；admin 用传入的 creatorIds（创建考试时选定老师）
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        List<Long> effectiveCreatorIds = isAdmin ? creatorIds : java.util.List.of(userId);

        var page = questionService.pageList(
                pageRequest.getCurrent(), pageRequest.getSize(),
                categoryId, questionType, difficulty, keyword, effectiveCreatorIds);

        // 填充 categoryName
        if (!page.getRecords().isEmpty()) {
            List<Long> categoryIds = page.getRecords().stream()
                    .map(Question::getCategoryId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (!categoryIds.isEmpty()) {
                List<QuestionCategory> categories = categoryMapper.selectBatchIds(categoryIds);
                Map<Long, String> nameMap = categories.stream()
                        .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getCategoryName, (a, b) -> a));
                for (Question q : page.getRecords()) {
                    if (q.getCategoryId() != null) {
                        q.setCategoryName(nameMap.get(q.getCategoryId()));
                    }
                }
            }
        }

        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }

    /**
     * 获取题目详情（含选项）
     */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Question question = questionService.getById(id);
        if (question == null) {
            return Result.error("题目不存在");
        }
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("question", question);
        // 查询选项
        if (Arrays.asList(1, 2).contains(question.getQuestionType())) {
            List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, id)
                    .orderByAsc(QuestionOption::getSort)
            );
            data.put("options", options);
        }
        return Result.success(data);
    }

    /**
     * 学生练习 - 分页获取可练习题目
     * 仅返回 status=1 且 allowPractice=1 的题目
     */
    @GetMapping("/practice")
    public Result<PageResult<Question>> practiceList(
            PageRequest pageRequest,
            @RequestParam(required = false) Long categoryId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 只查询允许练习的题目
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Question> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Question::getStatus, 1)
               .eq(Question::getAllowPractice, 1);
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Question::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Question::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Question> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        var page = questionService.page(p, wrapper);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }

    /**
     * 创建题目（教师/管理员）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PostMapping("/create")
    public Result<?> create(@RequestBody Question question, HttpServletRequest request) {
        question.setCreatorId((Long) request.getAttribute("userId"));
        question.setCreatedAt(new java.util.Date());
        question.setUpdatedAt(new java.util.Date());
        question.setStatus(1);
        if (question.getTitle() == null || question.getTitle().isEmpty()) {
            return Result.badRequest("题干不能为空");
        }
        questionService.save(question);

        // 保存选项（单选/多选）
        if (question.getQuestionType() != null && (question.getQuestionType() == 1 || question.getQuestionType() == 2)) {
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                for (int i = 0; i < question.getOptions().size(); i++) {
                    QuestionOption opt = new QuestionOption();
                    opt.setQuestionId(question.getId());
                    opt.setOptionLabel(String.valueOf((char) ('A' + i)));
                    opt.setOptionContent(question.getOptions().get(i).getOptionContent());
                    opt.setIsCorrect(0);
                    opt.setSort(i);
                    questionOptionMapper.insert(opt);
                }
            }
        }
        return Result.success("创建成功", question.getId());
    }

    /**
     * 更新题目（教师/管理员）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PutMapping("/update")
    public Result<?> update(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Question question = questionService.getById(id);
        if (question == null) {
            return Result.error("题目不存在");
        }

        if (params.containsKey("categoryId")) question.setCategoryId(Long.valueOf(params.get("categoryId").toString()));
        if (params.containsKey("questionType")) question.setQuestionType((Integer) params.get("questionType"));
        if (params.containsKey("difficulty")) question.setDifficulty((Integer) params.get("difficulty"));
        if (params.containsKey("title")) question.setTitle((String) params.get("title"));
        if (params.containsKey("answer")) question.setAnswer((String) params.get("answer"));
        if (params.containsKey("answerAnalysis")) question.setAnswerAnalysis((String) params.get("answerAnalysis"));
        question.setUpdatedAt(new java.util.Date());
        questionService.updateById(question);

        // 更新选项（单选/多选）
        Integer qType = question.getQuestionType();
        if (qType != null && (qType == 1 || qType == 2)) {
            // 删除旧选项
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, id));
            // 插入新选项
            String[] labels = {"A", "B", "C", "D", "E", "F"};
            for (int i = 0; i < labels.length; i++) {
                String key = "option" + labels[i];
                if (params.containsKey(key) && params.get(key) != null) {
                    String value = params.get(key).toString().trim();
                    if (!value.isEmpty()) {
                        QuestionOption opt = new QuestionOption();
                        opt.setQuestionId(id);
                        opt.setOptionLabel(labels[i]);
                        opt.setOptionContent(value);
                        opt.setSort(i);
                        opt.setIsCorrect(0); // 默认非正确答案
                        opt.setCreatedAt(new java.util.Date());
                        opt.setUpdatedAt(new java.util.Date());
                        questionOptionMapper.insert(opt);
                    }
                }
            }
        }

        return Result.success("更新成功");
    }

    /**
     * 删除题目（逻辑删除）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        questionService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 智能随机组卷
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PostMapping("/random-paper")
    public Result<List<Question>> randomPaper(@RequestBody Map<String, Object> params) {
        Long categoryId = params.containsKey("categoryId") ?
                Long.valueOf(params.get("categoryId").toString()) : null;
        Integer totalCount = Integer.valueOf(params.get("totalCount").toString());

        @SuppressWarnings("unchecked")
        Map<Integer, Integer> typeCountMap = (Map<Integer, Integer>) params.get("typeCountMap");
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> difficultyPercentMap = (Map<Integer, Integer>) params.get("difficultyPercentMap");

        @SuppressWarnings("unchecked")
        List<Object> rawCreatorIds = (List<Object>) params.get("creatorIds");
        List<Long> creatorIds = rawCreatorIds == null ? null :
                rawCreatorIds.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());

        List<Question> questions = questionService.randomPaper(
                categoryId, totalCount, typeCountMap, difficultyPercentMap, creatorIds);
        return Result.success("组卷成功", questions);
    }

    // ==================== 题库扩展：标签/解析/收藏 ====================

    /**
     * 标签列表（可选 keyword）
     */
    @GetMapping("/tags")
    public Result<List<QuestionTag>> tags(@RequestParam(required = false) String keyword) {
        return Result.success(questionTagService.listTags(keyword));
    }

    /**
     * 获取题目已打的标签
     */
    @GetMapping("/{id}/tags")
    public Result<List<QuestionTag>> questionTags(@PathVariable Long id) {
        return Result.success(questionTagService.listTagsOfQuestion(id));
    }

    /**
     * 给题目打标签（body: {tagIds:[]})
     */
    @PostMapping("/{id}/tags")
    public Result<?> addTags(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> tagIds = ((List<Object>) body.getOrDefault("tagIds", List.of()))
                .stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
        questionTagService.tagQuestion(id, tagIds);
        return Result.success("打标签成功");
    }

    /**
     * 取消题目标签
     */
    @DeleteMapping("/{id}/tags/{tagId}")
    public Result<?> removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        questionTagService.untagQuestion(id, tagId);
        return Result.success("取消标签成功");
    }

    /**
     * 获取题目解析
     */
    @GetMapping("/{id}/analysis")
    public Result<QuestionAnalysis> getAnalysis(@PathVariable Long id) {
        QuestionAnalysis analysis = questionAnalysisService.getByQuestionId(id);
        return Result.success(analysis);
    }

    /**
     * 保存/更新解析（body: QuestionAnalysis）
     */
    @PostMapping("/{id}/analysis")
    public Result<QuestionAnalysis> saveAnalysis(@PathVariable Long id, @RequestBody QuestionAnalysis body) {
        body.setQuestionId(id);
        QuestionAnalysis saved = questionAnalysisService.saveOrUpdateByQuestionId(body);
        return Result.success("保存成功", saved);
    }

    /**
     * 收藏题目（body: {questionId, note}）
     */
    @PostMapping("/favorite")
    public Result<QuestionFavorite> favorite(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long questionId = Long.valueOf(body.get("questionId").toString());
        String note = body.get("note") != null ? body.get("note").toString() : null;
        QuestionFavorite fav = questionFavoriteService.favorite(userId, questionId, note);
        return Result.success("收藏成功", fav);
    }

    /**
     * 我的收藏分页（page/size）
     */
    @GetMapping("/favorites")
    public Result<PageResult<QuestionFavorite>> favorites(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var result = questionFavoriteService.listMyFavorites(userId, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(),
                result.getSize(), result.getCurrent()));
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/favorite/{questionId}")
    public Result<?> unfavorite(@PathVariable Long questionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        questionFavoriteService.unfavorite(userId, questionId);
        return Result.success("取消收藏成功");
    }
}
