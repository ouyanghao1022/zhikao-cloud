-- 修复无效考试 session
-- 问题：submitExam 未校验空答案，导致 status=1/3、total_score=0 且无 exam_answer 的脏数据
-- 只删除 score=0 且无答题记录的 session；保留 status=2（待阅）的测试数据

-- 1. 删除空提交（score=0 且无答题记录）
DELETE FROM exam_session
WHERE status IN (1, 3)
AND total_score = 0
AND NOT EXISTS (SELECT 1 FROM exam_answer ea WHERE ea.session_id = exam_session.id);

-- 2. 重算受影响试卷的统计数据
UPDATE exam_paper ep
SET enrolled_count = COALESCE((
    SELECT COUNT(DISTINCT es.user_id) FROM exam_session es
    WHERE es.paper_id = ep.id AND es.status IN (1, 2, 3)
), 0),
avg_score = COALESCE((
    SELECT ROUND(AVG(es.total_score), 2) FROM exam_session es
    WHERE es.paper_id = ep.id AND es.status IN (1, 2, 3)
), 0),
max_score = COALESCE((
    SELECT MAX(es.total_score) FROM exam_session es
    WHERE es.paper_id = ep.id AND es.status IN (1, 2, 3)
), 0),
min_score = COALESCE((
    SELECT MIN(es.total_score) FROM exam_session es
    WHERE es.paper_id = ep.id AND es.status IN (1, 2, 3)
), 0);
