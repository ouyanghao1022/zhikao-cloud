package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.Question;
import com.zhikao.entity.WrongNotebook;
import com.zhikao.entity.WrongNoteTag;
import com.zhikao.entity.WrongReviewPlan;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.mapper.WrongNotebookMapper;
import com.zhikao.mapper.WrongNoteTagMapper;
import com.zhikao.mapper.WrongReviewPlanMapper;
import com.zhikao.service.WrongNotebookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 错题本Service实现
 */
@Service
public class WrongNotebookServiceImpl extends ServiceImpl<WrongNotebookMapper, WrongNotebook> implements WrongNotebookService {

    /** 艾宾浩斯复习间隔序列（天） */
    private static final int[] REVIEW_INTERVALS = {1, 2, 4, 7, 15, 30};

    @Autowired
    private WrongNoteTagMapper wrongNoteTagMapper;

    @Autowired
    private WrongReviewPlanMapper wrongReviewPlanMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public WrongNotebook addWrongNote(Long userId, Long questionId, Long examSessionId,
                                       String wrongAnswer, String correctAnswer) {
        // 检查是否已存在
        LambdaQueryWrapper<WrongNotebook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongNotebook::getUserId, userId)
               .eq(WrongNotebook::getQuestionId, questionId);
        WrongNotebook existing = getOne(wrapper);

        if (existing != null) {
            // 已存在则更新错误次数
            existing.setReviewCount(existing.getReviewCount() + 1);
            existing.setWrongAnswer(wrongAnswer);
            existing.setUpdatedAt(new Date());
            updateById(existing);
            return existing;
        }

        // 新增错题，初始化下次复习时间（按遗忘曲线）
        WrongNotebook note = new WrongNotebook();
        note.setUserId(userId);
        note.setQuestionId(questionId);
        note.setExamSessionId(examSessionId);
        note.setWrongAnswer(wrongAnswer);
        note.setCorrectAnswer(correctAnswer);
        note.setErrorType(0);
        note.setMasterStatus(0); // 未掌握
        note.setReviewCount(1);
        // 新增即设下次复习时间为 1 天后
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, calcNextReviewTime(1));
        note.setNextReviewTime(cal.getTime());
        note.setCreatedAt(new Date());
        note.setUpdatedAt(new Date());
        save(note);
        return note;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tagWrongNote(Long noteId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        // 先查已有的，重复忽略
        LambdaQueryWrapper<WrongNoteTag> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(WrongNoteTag::getWrongNoteId, noteId);
        List<WrongNoteTag> existList = wrongNoteTagMapper.selectList(existWrapper);
        Set<String> existNames = existList.stream()
                .map(WrongNoteTag::getTagName)
                .collect(Collectors.toSet());

        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }
            String name = tag.trim();
            if (existNames.contains(name)) {
                continue;
            }
            WrongNoteTag wnt = new WrongNoteTag();
            wnt.setWrongNoteId(noteId);
            wnt.setTagName(name);
            wnt.setCreatedAt(new Date());
            wrongNoteTagMapper.insert(wnt);
            existNames.add(name);
        }
    }

    @Override
    public List<String> listWrongNoteTags(Long noteId) {
        LambdaQueryWrapper<WrongNoteTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongNoteTag::getWrongNoteId, noteId)
               .orderByDesc(WrongNoteTag::getCreatedAt);
        List<WrongNoteTag> tags = wrongNoteTagMapper.selectList(wrapper);
        return tags.stream().map(WrongNoteTag::getTagName).collect(Collectors.toList());
    }

    @Override
    public int calcNextReviewTime(int reviewCount) {
        if (reviewCount <= 0) {
            return REVIEW_INTERVALS[0];
        }
        int idx = reviewCount - 1;
        if (idx >= REVIEW_INTERVALS.length) {
            return REVIEW_INTERVALS[REVIEW_INTERVALS.length - 1];
        }
        return REVIEW_INTERVALS[idx];
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateTodayPlan(Long userId) {
        // 今日 00:00
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        Date today = todayStart.getTime();

        // 查 next_review_time<=今天 且 masterStatus<2 的错题
        LambdaQueryWrapper<WrongNotebook> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(WrongNotebook::getUserId, userId)
                   .le(WrongNotebook::getNextReviewTime, today)
                   .lt(WrongNotebook::getMasterStatus, 2);
        long shouldReviewCount = count(noteWrapper);

        // 查今日计划记录（uk_user_date）
        LambdaQueryWrapper<WrongReviewPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(WrongReviewPlan::getUserId, userId)
                   .eq(WrongReviewPlan::getReviewDate, today);
        WrongReviewPlan todayPlan = wrongReviewPlanMapper.selectOne(planWrapper);

        if (todayPlan != null) {
            // 已存在则更新应复习数
            todayPlan.setReviewCount((int) shouldReviewCount);
            todayPlan.setUpdatedAt(new Date());
            wrongReviewPlanMapper.updateById(todayPlan);
        } else {
            todayPlan = new WrongReviewPlan();
            todayPlan.setUserId(userId);
            todayPlan.setReviewDate(today);
            todayPlan.setReviewCount((int) shouldReviewCount);
            todayPlan.setCompletedCount(0);
            todayPlan.setCreatedAt(new Date());
            todayPlan.setUpdatedAt(new Date());
            wrongReviewPlanMapper.insert(todayPlan);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", todayPlan.getId());
        result.put("reviewDate", todayPlan.getReviewDate());
        result.put("reviewCount", todayPlan.getReviewCount());
        result.put("completedCount", todayPlan.getCompletedCount());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WrongNotebook markReviewed(Long noteId) {
        WrongNotebook note = getById(noteId);
        if (note == null) {
            throw new IllegalArgumentException("错题记录不存在: " + noteId);
        }

        // reviewCount+1
        int newCount = (note.getReviewCount() == null ? 0 : note.getReviewCount()) + 1;
        note.setReviewCount(newCount);
        note.setLastReviewTime(new Date());

        // 重算 nextReviewTime
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, calcNextReviewTime(newCount));
        note.setNextReviewTime(cal.getTime());

        // 掌握状态升级：>=5 升为2(已掌握)、>=3 升为1(模糊)
        if (newCount >= 5) {
            note.setMasterStatus(2);
        } else if (newCount >= 3) {
            note.setMasterStatus(1);
        }
        note.setUpdatedAt(new Date());
        updateById(note);

        // 同步 wrong_review_plan.completedCount+1（今日计划）
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        Date today = todayStart.getTime();
        LambdaQueryWrapper<WrongReviewPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(WrongReviewPlan::getUserId, note.getUserId())
                   .eq(WrongReviewPlan::getReviewDate, today);
        WrongReviewPlan todayPlan = wrongReviewPlanMapper.selectOne(planWrapper);
        if (todayPlan != null) {
            todayPlan.setCompletedCount((todayPlan.getCompletedCount() == null ? 0 : todayPlan.getCompletedCount()) + 1);
            todayPlan.setUpdatedAt(new Date());
            wrongReviewPlanMapper.updateById(todayPlan);
        }

        return note;
    }

    @Override
    public Map<String, Object> getTodayPlan(Long userId) {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        Date today = todayStart.getTime();

        // 今日计划记录
        LambdaQueryWrapper<WrongReviewPlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(WrongReviewPlan::getUserId, userId)
                   .eq(WrongReviewPlan::getReviewDate, today);
        WrongReviewPlan todayPlan = wrongReviewPlanMapper.selectOne(planWrapper);

        int reviewCount = todayPlan != null && todayPlan.getReviewCount() != null ? todayPlan.getReviewCount() : 0;
        int completedCount = todayPlan != null && todayPlan.getCompletedCount() != null ? todayPlan.getCompletedCount() : 0;

        // 待复习错题列表
        LambdaQueryWrapper<WrongNotebook> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(WrongNotebook::getUserId, userId)
                   .le(WrongNotebook::getNextReviewTime, today)
                   .lt(WrongNotebook::getMasterStatus, 2)
                   .orderByAsc(WrongNotebook::getNextReviewTime);
        List<WrongNotebook> pendingNotes = list(noteWrapper);

        // 批量填充题目信息
        List<Long> qids = pendingNotes.stream()
                .map(WrongNotebook::getQuestionId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Question> qmap = Collections.emptyMap();
        if (!qids.isEmpty()) {
            List<Question> questions = questionMapper.selectBatchIds(qids);
            qmap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        }

        List<Map<String, Object>> pendingList = new ArrayList<>();
        for (WrongNotebook wn : pendingNotes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", wn.getId());
            item.put("questionId", wn.getQuestionId());
            item.put("wrongAnswer", wn.getWrongAnswer());
            item.put("correctAnswer", wn.getCorrectAnswer());
            item.put("masterStatus", wn.getMasterStatus());
            item.put("reviewCount", wn.getReviewCount());
            item.put("nextReviewTime", wn.getNextReviewTime());
            Question q = qmap.get(wn.getQuestionId());
            if (q != null) {
                item.put("title", q.getTitle());
                item.put("questionType", q.getQuestionType());
                item.put("difficulty", q.getDifficulty());
            }
            pendingList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reviewDate", today);
        result.put("reviewCount", reviewCount);
        result.put("completedCount", completedCount);
        result.put("pendingCount", pendingNotes.size());
        result.put("pendingNotes", pendingList);
        return result;
    }
}
