package com.zhikao.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhikao.entity.ExamPaper;
import com.zhikao.mapper.ExamPaperMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 启动时修复"已发布但结束时间已过期"的考试：
 * 将 status=1 且 end_time<当前时间 的考试 end_time 置空（长期有效）。
 * 用于修复创建考试时误选当天日期（默认 00:00 已过）导致无法参加的问题。
 * 只影响"已发布且已过期"的考试，不会动未发布/已结束/未来结束的考试。
 */
@Component
public class ExamStartupFixer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExamStartupFixer.class);

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Override
    public void run(String... args) {
        try {
            LambdaUpdateWrapper<ExamPaper> w = new LambdaUpdateWrapper<>();
            w.eq(ExamPaper::getStatus, 1)
             .isNotNull(ExamPaper::getEndTime)
             .lt(ExamPaper::getEndTime, new Date())
             .setSql("end_time = NULL");
            int n = examPaperMapper.update(null, w);
            if (n > 0) {
                log.info("[StartupFix] 已清空 {} 场已发布但已过期的考试结束时间（end_time=NULL，改为长期有效）", n);
            }
        } catch (Exception e) {
            log.warn("[StartupFix] 修复过期考试失败: {}", e.getMessage());
        }
    }
}
