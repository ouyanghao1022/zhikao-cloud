package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.*;
import com.zhikao.mapper.ClassMemberMapper;
import com.zhikao.mapper.ExamPaperClassMapper;
import com.zhikao.mapper.ExamPaperMapper;
import com.zhikao.mapper.ExamPaperQuestionMapper;
import com.zhikao.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 试卷Service实现类
 */
@Service
public class ExamPaperServiceImpl extends ServiceImpl<ExamPaperMapper, ExamPaper> implements ExamPaperService {

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private ExamAnswerService examAnswerService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionOptionService questionOptionService;

    @Autowired
    private WrongNotebookService wrongNotebookService;

    @Autowired
    private ClassMemberMapper classMemberMapper;

    @Autowired
    private SysMessageService sysMessageService;

    @Autowired
    private ExamPaperClassMapper examPaperClassMapper;

    @Autowired
    private com.zhikao.mapper.UserMapper userMapper;

    @Autowired
    private AiGradingService aiGradingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaper(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId);
        examPaperQuestionMapper.delete(wrapper);
        removeById(paperId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaper(Long paperId, ExamPaper input,
                            List<Long> questionIds, List<String> questionScores) {
        ExamPaper paper = getById(paperId);
        if (paper == null) throw new RuntimeException("试卷不存在");

        int status = paper.getStatus() != null ? paper.getStatus() : 0;
        if (status == 2) throw new RuntimeException("已结束的试卷不可编辑");

        // 检查是否有作答记录
        long sessionCount = examSessionService.count(
                new LambdaQueryWrapper<ExamSession>().eq(ExamSession::getPaperId, paperId));
        boolean hasSessions = sessionCount > 0;

        // ===== 基本信息 =====
        if (input.getTitle() != null) paper.setTitle(input.getTitle());
        if (input.getDescription() != null) paper.setDescription(input.getDescription());
        if (input.getDuration() != null) paper.setDuration(input.getDuration());
        if (input.getMaxScreenSwitch() != null) paper.setMaxScreenSwitch(input.getMaxScreenSwitch());

        // 总分/及格分/开始时间：有作答记录后锁定（仅 endTime 可延期）
        if (hasSessions) {
            if (input.getTotalScore() != null || input.getPassScore() != null || input.getStartTime() != null) {
                throw new RuntimeException("已有作答记录，总分/及格分/开始时间不可修改（仅可修改结束时间）");
            }
            if (input.getEndTime() != null) paper.setEndTime(input.getEndTime());
        } else {
            if (input.getTotalScore() != null) paper.setTotalScore(input.getTotalScore());
            if (input.getPassScore() != null) paper.setPassScore(input.getPassScore());
            if (input.getStartTime() != null) paper.setStartTime(input.getStartTime());
            if (input.getEndTime() != null) paper.setEndTime(input.getEndTime());
        }

        // ===== 固定组卷：替换题目（仅草稿 + 无作答） =====
        if (questionIds != null && !questionIds.isEmpty() && paper.getPaperType() != null && paper.getPaperType() == 1) {
            if (hasSessions) throw new RuntimeException("已有作答记录，不可修改题目");
            if (status != 0) throw new RuntimeException("仅草稿状态的固定试卷可替换题目");

            examPaperQuestionMapper.delete(
                    new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, paperId));
            java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
            List<Question> questions = questionService.listByIds(questionIds);
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                ExamPaperQuestion epq = new ExamPaperQuestion();
                epq.setPaperId(paperId);
                epq.setQuestionId(q.getId());
                epq.setSort(i + 1);
                java.math.BigDecimal score;
                if (questionScores != null && i < questionScores.size() && questionScores.get(i) != null) {
                    score = new java.math.BigDecimal(questionScores.get(i));
                } else {
                    score = q.getScore() != null ? q.getScore() : java.math.BigDecimal.ZERO;
                }
                epq.setScore(score);
                epq.setCreatedAt(new Date());
                examPaperQuestionMapper.insert(epq);
                totalScore = totalScore.add(score);
            }
            paper.setTotalScore(totalScore);
            // 按比例重算及格分
            if (input.getPassScore() == null && paper.getPassScore() != null) {
                java.math.BigDecimal originalPass = paper.getPassScore();
                java.math.BigDecimal oldTotal = input.getTotalScore() != null ? input.getTotalScore()
                        : (paper.getTotalScore() != null ? paper.getTotalScore() : java.math.BigDecimal.ONE);
                // 仅当 oldTotal>0 时重算
                if (oldTotal.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal ratio = originalPass.divide(oldTotal, 4, java.math.RoundingMode.HALF_UP);
                    paper.setPassScore(totalScore.multiply(ratio).setScale(0, java.math.RoundingMode.HALF_UP));
                }
            }
        }

        paper.setUpdatedAt(new Date());
        updateById(paper);
    }

    @Override
    public IPage<ExamPaper> pageList(long current, long size, Integer status, Long categoryId, Long classId, Long creatorId) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        // 按班级过滤时，强制只显示已发布的考试（学生端保护）
        if (classId != null) {
            wrapper.eq(ExamPaper::getStatus, 1);
        } else if (status != null) {
            wrapper.eq(ExamPaper::getStatus, status);
        }
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(ExamPaper::getCategoryId, categoryId);
        }
        if (creatorId != null) {
            wrapper.eq(ExamPaper::getCreatorId, creatorId);
        }
        if (classId != null) {
            wrapper.exists("SELECT 1 FROM exam_paper_class epc WHERE epc.paper_id = exam_paper.id AND epc.class_id = " + classId);
        }
        wrapper.orderByDesc(ExamPaper::getCreatedAt);
        return page(new Page<>(current, size), wrapper);
    }

    /**
     * 学生端：只显示发布到学生所在班级的考试
     */
    public IPage<ExamPaper> pageListForStudent(long current, long size, Long studentId) {
        // 查询学生所在的班级
        LambdaQueryWrapper<ClassMember> cmWrapper = new LambdaQueryWrapper<>();
        cmWrapper.eq(ClassMember::getUserId, studentId)
                 .eq(ClassMember::getStatus, 1);
        List<ClassMember> memberships = classMemberMapper.selectList(cmWrapper);
        List<Long> myClassIds = memberships.stream()
                .map(ClassMember::getClassId).distinct().toList();

        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getStatus, 1); // 只显示已发布

        if (myClassIds.isEmpty()) {
            // 学生不在任何班级 → 只显示无班级限制的考试（exam_paper_class 无记录）
            wrapper.notExists("SELECT 1 FROM exam_paper_class epc WHERE epc.paper_id = exam_paper.id");
        } else {
            // 显示发布到学生所在班级的考试 + 无班级限制的考试
            String ids = myClassIds.stream().map(String::valueOf)
                    .reduce((a, b) -> a + "," + b).orElse("0");
            wrapper.and(w -> w
                .exists("SELECT 1 FROM exam_paper_class epc WHERE epc.paper_id = exam_paper.id AND epc.class_id IN (" + ids + ")")
                .or()
                .notExists("SELECT 1 FROM exam_paper_class epc WHERE epc.paper_id = exam_paper.id")
            );
        }

        wrapper.orderByDesc(ExamPaper::getCreatedAt);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public Map<String, Object> getPaperDetail(Long paperId) {
        ExamPaper paper = getById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }
        // 填充创建人名称
        if (paper.getCreatorId() != null) {
            User creator = userMapper.selectById(paper.getCreatorId());
            if (creator != null) paper.setCreatorName(creator.getNickname());
        }

        // 解析出题老师姓名
        List<Long> tIds = parseTeacherIds(paper.getTeacherIds());
        List<String> teacherNames = new ArrayList<>();
        if (tIds != null && !tIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(tIds)) {
                if (u != null) teacherNames.add(u.getNickname() != null ? u.getNickname() : u.getUsername());
            }
        }

        List<Map<String, Object>> questions = loadPaperQuestions(paperId);

        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", questions);
        data.put("teacherNames", teacherNames);
        return data;
    }

    /**
     * 加载试卷题目列表（优先实时数据，题目被删则使用快照）
     */
    private List<Map<String, Object>> loadPaperQuestions(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> epqWrapper = new LambdaQueryWrapper<>();
        epqWrapper.eq(ExamPaperQuestion::getPaperId, paperId).orderByAsc(ExamPaperQuestion::getSort);
        List<ExamPaperQuestion> epqList = examPaperQuestionMapper.selectList(epqWrapper);

        List<Long> qids = epqList.stream().map(ExamPaperQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = new HashMap<>();
        if (!qids.isEmpty()) {
            List<Question> existingQuestions = questionService.listByIds(qids);
            for (Question q : existingQuestions) questionMap.put(q.getId(), q);
        }

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion epq : epqList) {
            Map<String, Object> qd = new HashMap<>();
            Question liveQ = questionMap.get(epq.getQuestionId());

            if (liveQ != null) {
                qd.put("id", liveQ.getId());
                qd.put("title", liveQ.getTitle());
                qd.put("questionType", liveQ.getQuestionType());
                qd.put("difficulty", liveQ.getDifficulty());
                qd.put("answer", liveQ.getAnswer());
                qd.put("answerAnalysis", liveQ.getAnswerAnalysis());
                qd.put("categoryId", liveQ.getCategoryId());
                qd.put("content", liveQ.getContent());
                // 单选/多选：加载选项
                if (liveQ.getQuestionType() != null && (liveQ.getQuestionType() == 1 || liveQ.getQuestionType() == 2)) {
                    List<QuestionOption> opts = questionOptionService.list(
                            new LambdaQueryWrapper<QuestionOption>()
                                    .eq(QuestionOption::getQuestionId, liveQ.getId())
                                    .orderByAsc(QuestionOption::getSort));
                    qd.put("options", opts);
                }
            } else if (epq.getQuestionSnapshot() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> snapshot = om.readValue(epq.getQuestionSnapshot(), Map.class);
                    qd.putAll(snapshot);
                    qd.put("_fromSnapshot", true);
                } catch (Exception e) {
                    qd.put("title", "(题目数据丢失)");
                    qd.put("id", epq.getQuestionId());
                }
            } else {
                qd.put("title", "(题目已删除)");
                qd.put("id", epq.getQuestionId());
            }
            qd.put("score", epq.getScore());
            qd.put("sort", epq.getSort());
            questions.add(qd);
        }
        return questions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPaper(ExamPaper paper, List<Long> questionIds,
                            List<String> questionScores,
                            Map<String, Object> randomConfig) {
        paper.setStatus(0); // 草稿状态
        paper.setEnrolledCount(0);
        paper.setAvgScore(java.math.BigDecimal.ZERO);
        paper.setMaxScore(java.math.BigDecimal.ZERO);
        paper.setMinScore(java.math.BigDecimal.ZERO);
        paper.setCreatedAt(new Date());
        paper.setUpdatedAt(new Date());
        // 持久化随机组卷配置，用于"重新抽题"
        if (randomConfig != null) {
            try {
                paper.setRandomConfig(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(randomConfig));
            } catch (Exception ignore) { /* 序列化失败不影响主流程 */ }
        }
        save(paper);

        List<Question> selectedQuestions;

        if (paper.getPaperType() != null && paper.getPaperType() == 2 && randomConfig != null) {
            // ===== 随机组卷 =====
            Long categoryId = randomConfig.containsKey("categoryId") ?
                    Long.valueOf(randomConfig.get("categoryId").toString()) : null;
            Integer totalCount = Integer.valueOf(randomConfig.get("totalCount").toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> rawTypeCountMap =
                    (Map<String, Object>) randomConfig.get("typeCountMap");
            // JSON Map 的 key 是 String，需要转为 Integer
            Map<Integer, Integer> typeCountMap = new java.util.HashMap<>();
            if (rawTypeCountMap != null) {
                rawTypeCountMap.forEach((k, v) -> 
                    typeCountMap.put(Integer.valueOf(k), Integer.valueOf(v.toString())));
            }

            Map<Integer, Integer> difficultyPercentMap = null;

            List<Long> creatorIds = parseTeacherIds(paper.getTeacherIds());
            selectedQuestions = questionService.randomPaper(
                    categoryId, totalCount, typeCountMap, difficultyPercentMap, creatorIds);
        } else {
            // ===== 固定组卷 =====
            if (questionIds == null || questionIds.isEmpty()) {
                throw new RuntimeException("请选择至少一道题目");
            }
            selectedQuestions = questionService.listByIds(questionIds);
        }

        // 保存试卷-题目关联
        java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question q = selectedQuestions.get(i);
            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setPaperId(paper.getId());
            epq.setQuestionId(q.getId());
            epq.setSort(i + 1);

            // 分值：优先使用传入的分值，否则使用题目默认分值
            java.math.BigDecimal score;
            if (questionScores != null && i < questionScores.size() && questionScores.get(i) != null) {
                score = new java.math.BigDecimal(questionScores.get(i));
            } else {
                score = q.getScore() != null ? q.getScore() : java.math.BigDecimal.ZERO;
            }
            epq.setScore(score);
            
            // 保存题目快照（JSON），确保题库删除后试卷仍完整
            try {
                java.util.Map<String, Object> snapshot = new java.util.HashMap<>();
                snapshot.put("id", q.getId());
                snapshot.put("title", q.getTitle());
                snapshot.put("questionType", q.getQuestionType());
                snapshot.put("difficulty", q.getDifficulty());
                snapshot.put("answer", q.getAnswer());
                snapshot.put("answerAnalysis", q.getAnswerAnalysis());
                snapshot.put("categoryId", q.getCategoryId());
                if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                    snapshot.put("options", q.getOptions());
                }
                epq.setQuestionSnapshot(objectMapper.writeValueAsString(snapshot));
            } catch (Exception ignore) {
                // 快照失败不影响主流程
            }
            
            epq.setCreatedAt(new Date());
            examPaperQuestionMapper.insert(epq);

            totalScore = totalScore.add(score);
        }

        // 更新试卷总分为实际题目分值总和；按用户设定的及格比例重算及格分，
        // 避免实际总分（题目分值累加）与输入总分不一致时及格线失衡（如输入100/60，实际70→及格42而非60）
        java.math.BigDecimal originalTotal = paper.getTotalScore();
        java.math.BigDecimal originalPass = paper.getPassScore();
        paper.setTotalScore(totalScore);
        if (originalTotal != null && originalTotal.compareTo(java.math.BigDecimal.ZERO) > 0 && originalPass != null) {
            java.math.BigDecimal ratio = originalPass.divide(originalTotal, 4, java.math.RoundingMode.HALF_UP);
            paper.setPassScore(totalScore.multiply(ratio).setScale(0, java.math.RoundingMode.HALF_UP));
        }
        updateById(paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reassembleRandomPaper(Long paperId) {
        ExamPaper paper = getById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }
        if (paper.getPaperType() == null || paper.getPaperType() != 2) {
            throw new RuntimeException("仅随机组卷试卷可重新抽题");
        }
        if (paper.getStatus() == null || paper.getStatus() != 0) {
            throw new RuntimeException("仅草稿状态试卷可重新抽题");
        }
        if (paper.getRandomConfig() == null || paper.getRandomConfig().isBlank()) {
            throw new RuntimeException("随机组卷配置缺失，无法重新抽题");
        }

        Map<String, Object> randomConfig;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(paper.getRandomConfig(), Map.class);
            randomConfig = parsed;
        } catch (Exception e) {
            throw new RuntimeException("随机组卷配置损坏，无法重新抽题");
        }

        Long categoryId = randomConfig.containsKey("categoryId") ?
                Long.valueOf(randomConfig.get("categoryId").toString()) : null;
        Integer totalCount = randomConfig.containsKey("totalCount") ?
                Integer.valueOf(randomConfig.get("totalCount").toString()) : 0;
        @SuppressWarnings("unchecked")
        Map<String, Object> rawTypeCountMap = (Map<String, Object>) randomConfig.get("typeCountMap");
        Map<Integer, Integer> typeCountMap = new HashMap<>();
        if (rawTypeCountMap != null) {
            rawTypeCountMap.forEach((k, v) -> typeCountMap.put(Integer.valueOf(k), Integer.valueOf(v.toString())));
        }
        List<Long> creatorIds = parseTeacherIds(paper.getTeacherIds());

        List<Question> selectedQuestions = questionService.randomPaper(
                categoryId, totalCount, typeCountMap, null, creatorIds);

        // 删除旧题目关联，插入新的
        examPaperQuestionMapper.delete(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, paperId));

        java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question q = selectedQuestions.get(i);
            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setPaperId(paper.getId());
            epq.setQuestionId(q.getId());
            epq.setSort(i + 1);
            epq.setScore(q.getScore() != null ? q.getScore() : java.math.BigDecimal.ZERO);
            epq.setCreatedAt(new Date());
            examPaperQuestionMapper.insert(epq);
            totalScore = totalScore.add(epq.getScore());
        }

        java.math.BigDecimal oldTotal = paper.getTotalScore();
        java.math.BigDecimal oldPass = paper.getPassScore();
        paper.setTotalScore(totalScore);
        // 重抽后实际总分变化，按原及格比例重算及格分
        if (oldTotal != null && oldTotal.compareTo(java.math.BigDecimal.ZERO) > 0 && oldPass != null) {
            java.math.BigDecimal ratio = oldPass.divide(oldTotal, 4, java.math.RoundingMode.HALF_UP);
            paper.setPassScore(totalScore.multiply(ratio).setScale(0, java.math.RoundingMode.HALF_UP));
        }
        paper.setUpdatedAt(new Date());
        updateById(paper);

        Map<String, Object> result = new HashMap<>();
        result.put("questionCount", selectedQuestions.size());
        result.put("totalScore", totalScore);
        result.put("passScore", paper.getPassScore());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPaper(Long paperId, List<Long> classIds) {
        ExamPaper paper = new ExamPaper();
        paper.setId(paperId);
        paper.setStatus(1);
        paper.setUpdatedAt(new Date());
        updateById(paper);

        // 保存考试-班级绑定
        if (classIds != null && !classIds.isEmpty()) {
            for (Long classId : classIds) {
                ExamPaperClass binding = new ExamPaperClass();
                binding.setPaperId(paperId);
                binding.setClassId(classId);
                binding.setCreatedAt(new Date());
                examPaperClassMapper.insert(binding);
            }
        }

          // 发送考试通知给班级学生
          if (classIds != null && !classIds.isEmpty()) {
              ExamPaper paperEntity = getById(paperId);
              String paperTitle = paperEntity != null ? paperEntity.getTitle() : "";
              for (Long classId : classIds) {
                  LambdaQueryWrapper<ClassMember> cm = new LambdaQueryWrapper<ClassMember>()
                          .eq(ClassMember::getClassId, classId)
                          .eq(ClassMember::getRole, 0)
                          .eq(ClassMember::getStatus, 1);
                  List<ClassMember> students = classMemberMapper.selectList(cm);
                  for (ClassMember s : students) {
                      sysMessageService.sendMessage(s.getUserId(), 2,
                              "新考试发布：" + paperTitle,
                              "您有一场新考试《" + paperTitle + "》已发布，请及时参加！",
                              paperId, "/exam/take/" + paperId);
                  }
              }
          }
    }

    @Override
    public Map<String, Object> getExamForTaking(Long paperId, Long userId) {
        ExamPaper paper = getById(paperId);
        if (paper == null || paper.getStatus() != 1) {
            throw new RuntimeException("试卷不可用");
        }

        // 检查学生是否已提交过此试卷
        LambdaQueryWrapper<ExamSession> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ExamSession::getPaperId, paperId)
                     .eq(ExamSession::getUserId, userId)
                     .eq(ExamSession::getStatus, 1);
        long submittedCount = examSessionService.count(checkWrapper);
        if (submittedCount > 0) {
            throw new RuntimeException("你已参加过此考试");
        }

        // 检查考试是否在有效时间范围内
        Date now = new Date();
        if (paper.getStartTime() != null && now.before(paper.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }
        if (paper.getEndTime() != null && now.after(paper.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }

        // 检查学生是否在考试绑定的班级中
        LambdaQueryWrapper<ExamPaperClass> epcWrapper = new LambdaQueryWrapper<>();
        epcWrapper.eq(ExamPaperClass::getPaperId, paperId);
        List<ExamPaperClass> bindings = examPaperClassMapper.selectList(epcWrapper);
        if (bindings != null && !bindings.isEmpty()) {
            List<Long> boundClassIds = bindings.stream()
                    .map(ExamPaperClass::getClassId).distinct().toList();
            LambdaQueryWrapper<ClassMember> cmWrapper = new LambdaQueryWrapper<>();
            cmWrapper.eq(ClassMember::getUserId, userId)
                     .eq(ClassMember::getStatus, 1)
                     .in(ClassMember::getClassId, boundClassIds);
            Long memberCount = classMemberMapper.selectCount(cmWrapper);
            if (memberCount == null || memberCount == 0) {
                throw new RuntimeException("你不在该考试的班级范围内，无法参加");
            }
        }

        // 获取题目列表（含快照兜底）
        List<Map<String, Object>> questions = loadPaperQuestions(paperId);

        // 如果需要乱序
        if (paper.getShuffleQuestion() != null && paper.getShuffleQuestion() == 1) {
            Collections.shuffle(questions);
        }

        // 脱敏：不返回正确答案和选项正确标记
        for (Map<String, Object> q : questions) {
            q.remove("answer");
            q.remove("answerAnalysis");
            Object opts = q.get("options");
            if (opts instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> optList = (List<Object>) opts;
                for (Object o : optList) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> om = (Map<String, Object>) o;
                        om.remove("isCorrect");
                    }
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", questions);
        data.put("startTime", now);
        data.put("duration", paper.getDuration());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitExam(Long paperId, Long userId, List<Map<String, Object>> answers) {
        ExamPaper paper = getById(paperId);
        if (paper == null || paper.getStatus() != 1) {
            throw new RuntimeException("试卷不可用");
        }

        // 检查班级权限（与 takeExam 保持一致）
        LambdaQueryWrapper<ExamPaperClass> epcWrapper = new LambdaQueryWrapper<>();
        epcWrapper.eq(ExamPaperClass::getPaperId, paperId);
        List<ExamPaperClass> bindings = examPaperClassMapper.selectList(epcWrapper);
        if (bindings != null && !bindings.isEmpty()) {
            List<Long> boundClassIds = bindings.stream()
                    .map(ExamPaperClass::getClassId).distinct().toList();
            LambdaQueryWrapper<ClassMember> cmWrapper = new LambdaQueryWrapper<>();
            cmWrapper.eq(ClassMember::getUserId, userId)
                     .eq(ClassMember::getStatus, 1)
                     .in(ClassMember::getClassId, boundClassIds);
            Long memberCount = classMemberMapper.selectCount(cmWrapper);
            if (memberCount == null || memberCount == 0) {
                throw new RuntimeException("你不在该考试的班级范围内，无法提交");
            }
        }

        // 创建或更新考试记录
        ExamSession session = examSessionService.getOrCreateSession(paperId, userId);
        session.setSubmitTime(new Date());
        session.setStatus(1); // 已提交
        session.setIpAddress("");

        // 记录学生所属班级
        if (session.getClassId() == null) {
            LambdaQueryWrapper<ClassMember> cmWrapper = new LambdaQueryWrapper<>();
            cmWrapper.eq(ClassMember::getUserId, userId)
                     .eq(ClassMember::getStatus, 1)
                     .orderByDesc(ClassMember::getJoinTime)
                     .last("LIMIT 1");
            ClassMember member = classMemberMapper.selectOne(cmWrapper);
            if (member != null) {
                session.setClassId(member.getClassId());
            }
        }

        examSessionService.updateById(session);

        java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
        java.math.BigDecimal objectiveScore = java.math.BigDecimal.ZERO;
        java.math.BigDecimal subjectiveScore = java.math.BigDecimal.ZERO;

        // 保存每题答案并自动判分
        boolean hasSubjective = false;
        for (Map<String, Object> ans : answers) {
            Long questionId = Long.valueOf(ans.get("questionId").toString());
            String userAnswer = ans.get("answer").toString();

            Question question = questionService.getById(questionId);
            if (question == null) {
                // 题目已被删除，跳过该答案
                continue;
            }
            ExamAnswer examAnswer = new ExamAnswer();
            examAnswer.setSessionId(session.getId());
            examAnswer.setQuestionId(questionId);
            examAnswer.setUserAnswer(userAnswer);
            examAnswer.setCreatedAt(new Date());

            // 客观题自动判分（1单选 2多选 3判断 4填空）
            if (Arrays.asList(1, 2, 3, 4).contains(question.getQuestionType())) {
                boolean isCorrect = checkAnswer(question, userAnswer);
                examAnswer.setIsCorrect(isCorrect ? 1 : 0);
                java.math.BigDecimal score = examPaperMapper.getQuestionScore(paperId, questionId);
                if (score == null) score = question.getScore() != null ? question.getScore() : java.math.BigDecimal.ZERO;
                examAnswer.setScore(isCorrect ? score : java.math.BigDecimal.ZERO);
                objectiveScore = objectiveScore.add(isCorrect ? score : java.math.BigDecimal.ZERO);
                totalScore = totalScore.add(isCorrect ? score : java.math.BigDecimal.ZERO);
            } else {
                // 主观题（5简答 6论述 7编程）—— AI 预评分，教师可复核
                hasSubjective = true;
                java.math.BigDecimal maxScore = examPaperMapper.getQuestionScore(paperId, questionId);
                if (maxScore == null) maxScore = question.getScore() != null ? question.getScore() : java.math.BigDecimal.ZERO;
                int maxInt = maxScore.intValue();
                var aiResult = aiGradingService.gradeSubjective(userAnswer, question.getAnswer(), maxInt);
                java.math.BigDecimal aiScore = new java.math.BigDecimal(aiResult.get("score").toString());
                examAnswer.setScore(aiScore);
                examAnswer.setIsCorrect(maxInt > 0 && aiScore.intValue() >= maxInt * 0.6 ? 1 : 0);
                subjectiveScore = subjectiveScore.add(aiScore);
                totalScore = totalScore.add(aiScore);
            }

            examAnswerService.save(examAnswer);

            // 如果答错，自动加入错题本
            if (examAnswer.getIsCorrect() != null && examAnswer.getIsCorrect() == 0) {
                wrongNotebookService.addWrongNote(userId, questionId, session.getId(), userAnswer, question.getAnswer());
            }
        }

        // 更新总分和状态
        session.setTotalScore(totalScore);
        session.setObjectiveScore(objectiveScore);
        session.setSubjectiveScore(subjectiveScore);
        // 全是客观题→已阅卷；有主观题→待阅卷(教师可复核AI预评分)
        session.setStatus(hasSubjective ? 2 : 3);
        session.setUpdatedAt(new Date());
        examSessionService.updateById(session);

        // 更新试卷参与统计
        LambdaQueryWrapper<ExamSession> statsWrapper = new LambdaQueryWrapper<>();
        statsWrapper.eq(ExamSession::getPaperId, paperId)
                    .in(ExamSession::getStatus, 1, 2, 3);
        List<ExamSession> allSessions = examSessionService.list(statsWrapper);
        long participantCount = allSessions.stream()
                .map(ExamSession::getUserId).distinct().count();
        paper.setEnrolledCount((int) participantCount);

        if (!allSessions.isEmpty()) {
            java.math.BigDecimal sum = allSessions.stream()
                    .map(s -> s.getTotalScore() != null ? s.getTotalScore() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            paper.setAvgScore(sum.divide(new java.math.BigDecimal(allSessions.size()), 2, java.math.RoundingMode.HALF_UP));
            paper.setMaxScore(allSessions.stream()
                    .map(s -> s.getTotalScore() != null ? s.getTotalScore() : java.math.BigDecimal.ZERO)
                    .max(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO));
            paper.setMinScore(allSessions.stream()
                    .map(s -> s.getTotalScore() != null ? s.getTotalScore() : java.math.BigDecimal.ZERO)
                    .min(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO));
        }
        updateById(paper);

        // 返回成绩与合格状态
        Map<String, Object> result = new HashMap<>();
        result.put("score", totalScore);
        result.put("totalScore", paper.getTotalScore());
        result.put("passed", totalScore.compareTo(paper.getPassScore() != null ? paper.getPassScore() : java.math.BigDecimal.ZERO) >= 0);
        result.put("passScore", paper.getPassScore());
        result.put("sessionId", session.getId());
        return result;
    }

    @Override
    public IPage<Map<String, Object>> getMyRecords(Long userId, long current, long size) {
        return examPaperMapper.selectMyRecords(userId, new Page<>(current, size));
    }

    /** 把 "1,13,34" 解析为 List<Long> */
    private List<Long> parseTeacherIds(String teacherIds) {
        if (teacherIds == null || teacherIds.isBlank()) return null;
        return java.util.Arrays.stream(teacherIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).toList();
    }

    /**
     * 校验答案是否正确
     */
    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null || question.getAnswer() == null) return false;

        int type = question.getQuestionType();
        if (type == 1 || type == 3 || type == 4) {
            // 单选/判断/填空：精确匹配
            return userAnswer.trim().equalsIgnoreCase(question.getAnswer().trim());
        } else if (type == 2) {
            // 多选：顺序无关匹配
            String[] correct = question.getAnswer().split(",");
            String[] user = userAnswer.split(",");
            Set<String> correctSet = new HashSet<>(Arrays.asList(correct));
            Set<String> userSet = new HashSet<>(Arrays.asList(user));
            return correctSet.equals(userSet);
        }
        return false;
    }
}
