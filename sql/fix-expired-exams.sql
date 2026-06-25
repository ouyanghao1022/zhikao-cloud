-- =====================================================
-- 修复"已发布但结束时间已过期"的考试
-- 将 status=1(已发布) 且 end_time<当前时间 的考试 end_time 清空（改为长期有效）
-- 用于修复创建考试时误选当天日期（默认 00:00 已过）导致无法参加的问题
-- 用法：mysql -uroot -p123456 < sql/fix-expired-exams.sql
-- =====================================================
USE zhikao_cloud;

-- 修复前：查看哪些考试已过期
SELECT id, title, status, start_time, end_time, NOW() AS now_time
FROM exam_paper
WHERE status = 1 AND end_time IS NOT NULL AND end_time < NOW();

-- 执行修复：清空已过期考试的结束时间
UPDATE exam_paper
SET end_time = NULL, updated_at = NOW()
WHERE status = 1 AND end_time IS NOT NULL AND end_time < NOW();

-- 修复后：确认
SELECT id, title, status, start_time, end_time
FROM exam_paper
WHERE status = 1;
