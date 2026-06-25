-- ============================================
-- 修复已加入班级的学生年级不一致问题
-- 将学生的 grade 同步为所在班级的 grade
-- 仅更新 role=0（学生），且年级为空或与班级不一致的记录
-- ============================================

UPDATE sys_user u
INNER JOIN class_member cm ON cm.user_id = u.id AND cm.status = 1 AND cm.role = 0
INNER JOIN sys_class c ON c.id = cm.class_id AND c.status = 1
SET u.grade = c.grade,
    u.updated_at = NOW()
WHERE c.grade IS NOT NULL 
  AND c.grade != ''
  AND (u.grade IS NULL OR u.grade = '' OR u.grade != c.grade);

-- 验证修复结果
-- SELECT u.id, u.username, u.grade AS user_grade, c.grade AS class_grade, c.class_name
-- FROM sys_user u
-- INNER JOIN class_member cm ON cm.user_id = u.id AND cm.status = 1 AND cm.role = 0
-- INNER JOIN sys_class c ON c.id = cm.class_id AND c.status = 1;
