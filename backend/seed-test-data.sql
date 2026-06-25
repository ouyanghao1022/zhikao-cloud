-- ============================================================
-- 智考云 全场景测试数据
-- 年级覆盖：小学、初中、高中、大学
-- 公共密码 (BCrypt for "123456")
-- ============================================================

SET NAMES utf8mb4;

SET @pwd = '$2b$10$yXdtQiOq54RUAxSXh2kw2.Y.E8iZzBts.PKkqZio2Eso33w0g.6hS';
SET @now = NOW();

-- ════════════════════════════════════════════════════════
-- 0. 修复现有用户缺少角色
-- ════════════════════════════════════════════════════════
INSERT IGNORE INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, 3, @now FROM sys_user u
WHERE u.username IN ('student2','student3') AND NOT EXISTS (
  SELECT 1 FROM sys_user_role WHERE user_id=u.id
);

-- ════════════════════════════════════════════════════════
-- 一、新增教师 (3人)
-- ════════════════════════════════════════════════════════

INSERT INTO sys_user (username, password, nickname, email, phone, gender, school, grade, status, created_at, updated_at, deleted) VALUES
('teacher2', @pwd, '王老师', 'wang@zhikao.com', '13800001001', 2, NULL, NULL, 1, @now, @now, 0),
('teacher3', @pwd, '李老师', 'li@zhikao.com', '13800001002', 1, NULL, NULL, 1, @now, @now, 0),
('teacher4', @pwd, '张教授', 'zhang@zhikao.com', '13800001003', 1, NULL, NULL, 1, @now, @now, 0);

INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT id, 2, @now FROM sys_user WHERE username IN ('teacher2','teacher3','teacher4');

SELECT '新增教师 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 二、新增学生 (13人，含测试用不加入班级的2人)
-- ════════════════════════════════════════════════════════

INSERT INTO sys_user (username, password, nickname, email, phone, gender, school, grade, status, created_at, updated_at, deleted) VALUES
-- 小学
('student4', @pwd, '小明', 'xm@test.com', '13800002001', 1, '阳光小学', '五年级', 1, @now, @now, 0),
('student5', @pwd, '小红', 'xh@test.com', '13800002002', 2, '阳光小学', '六年级', 1, @now, @now, 0),
-- 初中
('student6', @pwd, '小刚', 'xg@test.com', '13800002003', 1, '育才中学', '初一', 1, @now, @now, 0),
('student7', @pwd, '小丽', 'xl@test.com', '13800002004', 2, '育才中学', '初二', 1, @now, @now, 0),
('student8', @pwd, '小强', 'xq@test.com', '13800002005', 1, '育才中学', '初三', 1, @now, @now, 0),
-- 高中
('student9', @pwd, '小美', 'xmei@test.com', '13800002006', 2, '第一中学', '高一', 1, @now, @now, 0),
('student10', @pwd, '志远', 'zy@test.com', '13800002007', 1, '第一中学', '高二', 1, @now, @now, 0),
('student11', @pwd, '文博', 'wb@test.com', '13800002008', 1, '第一中学', '高二', 1, @now, @now, 0),
-- 大学
('student12', @pwd, '浩然', 'hr@test.com', '13800002009', 1, '科技大学', '大一', 1, @now, @now, 0),
('student13', @pwd, '雨桐', 'yt@test.com', '13800002010', 2, '科技大学', '大一', 1, @now, @now, 0),
('student14', @pwd, '思源', 'sy@test.com', '13800002011', 1, '科技大学', '大二', 1, @now, @now, 0),
-- ⚠️ 测试专用：以下2名学生不加入任何班级
('student15', @pwd, '测试学生A', 'testa@test.com', '13800002901', 1, NULL, NULL, 1, @now, @now, 0),
('student16', @pwd, '测试学生B', 'testb@test.com', '13800002902', 2, NULL, NULL, 1, @now, @now, 0);

-- 分配学生角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT id, 3, @now FROM sys_user
WHERE username BETWEEN 'student4' AND 'student16'
  AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id=sys_user.id);

SELECT '新增学生 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 三、变量准备 - 获取所有用户ID
-- ════════════════════════════════════════════════════════

SET @t2_id = (SELECT id FROM sys_user WHERE username='teacher2');
SET @t3_id = (SELECT id FROM sys_user WHERE username='teacher3');
SET @t4_id = (SELECT id FROM sys_user WHERE username='teacher4');
SET @t1_id = 13; -- teacher1 (已存在)

SET @s1_id  = 12; -- student1
SET @s2_id  = 15; -- student2 (无班级)
SET @s3_id  = 16; -- student3 (无班级)
SET @s4_id  = (SELECT id FROM sys_user WHERE username='student4');
SET @s5_id  = (SELECT id FROM sys_user WHERE username='student5');
SET @s6_id  = (SELECT id FROM sys_user WHERE username='student6');
SET @s7_id  = (SELECT id FROM sys_user WHERE username='student7');
SET @s8_id  = (SELECT id FROM sys_user WHERE username='student8');
SET @s9_id  = (SELECT id FROM sys_user WHERE username='student9');
SET @s10_id = (SELECT id FROM sys_user WHERE username='student10');
SET @s11_id = (SELECT id FROM sys_user WHERE username='student11');
SET @s12_id = (SELECT id FROM sys_user WHERE username='student12');
SET @s13_id = (SELECT id FROM sys_user WHERE username='student13');
SET @s14_id = (SELECT id FROM sys_user WHERE username='student14');
SET @s15_id = (SELECT id FROM sys_user WHERE username='student15');
SET @s16_id = (SELECT id FROM sys_user WHERE username='student16');

-- ════════════════════════════════════════════════════════
-- 四、创建班级 (10个：小学2 + 初中3 + 高中3 + 大学2)
-- ════════════════════════════════════════════════════════

INSERT INTO sys_class (class_name, class_code, teacher_id, school, grade, description, max_students, student_count, status, created_at, updated_at) VALUES
-- 小学 (teacher3 = 李老师)
('五年级1班',   UPPER(SUBSTRING(MD5(RAND()),1,6)), @t3_id, '阳光小学', '五年级', '五年级数学语文', 50, 0, 1, @now, @now),
('六年级2班',   UPPER(SUBSTRING(MD5(RAND()),1,6)), @t3_id, '阳光小学', '六年级', '六年级冲刺班', 50, 0, 1, @now, @now),
-- 初中 (teacher2 = 王老师)
('初一1班',     UPPER(SUBSTRING(MD5(RAND()),1,6)), @t2_id, '育才中学', '初一', '初一年级', 45, 0, 1, @now, @now),
('初二1班',     UPPER(SUBSTRING(MD5(RAND()),1,6)), @t2_id, '育才中学', '初二', '初二年级', 45, 0, 1, @now, @now),
('初三1班',     UPPER(SUBSTRING(MD5(RAND()),1,6)), @t2_id, '育才中学', '初三', '中考备战班', 40, 0, 1, @now, @now),
-- 高中 (teacher1 = 测试教师)
('高一1班',     UPPER(SUBSTRING(MD5(RAND()),1,6)), @t1_id, '第一中学', '高一', '高一年级', 50, 0, 1, @now, @now),
('高二理科班',  UPPER(SUBSTRING(MD5(RAND()),1,6)), @t1_id, '第一中学', '高二', '理科综合班', 45, 0, 1, @now, @now),
('高三冲刺班',  UPPER(SUBSTRING(MD5(RAND()),1,6)), @t1_id, '第一中学', '高三', '高考冲刺重点班', 40, 0, 1, @now, @now),
-- 大学 (teacher4 = 张教授)
('大一计算机科学', UPPER(SUBSTRING(MD5(RAND()),1,6)), @t4_id, '科技大学', '大一', '计算机系2026级', 60, 0, 1, @now, @now),
('大二数学系',     UPPER(SUBSTRING(MD5(RAND()),1,6)), @t4_id, '科技大学', '大二', '数学系2025级', 55, 0, 1, @now, @now);

SELECT '创建班级 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 五、学生加入班级
-- ════════════════════════════════════════════════════════

SET @c_51 = (SELECT id FROM sys_class WHERE class_name='五年级1班'   AND status=1);
SET @c_62 = (SELECT id FROM sys_class WHERE class_name='六年级2班'   AND status=1);
SET @c_71 = (SELECT id FROM sys_class WHERE class_name='初一1班'     AND status=1);
SET @c_81 = (SELECT id FROM sys_class WHERE class_name='初二1班'     AND status=1);
SET @c_91 = (SELECT id FROM sys_class WHERE class_name='初三1班'     AND status=1);
SET @c_101= (SELECT id FROM sys_class WHERE class_name='高一1班'     AND status=1);
SET @c_112= (SELECT id FROM sys_class WHERE class_name='高二理科班'  AND status=1);
SET @c_123= (SELECT id FROM sys_class WHERE class_name='高三冲刺班'  AND status=1);
SET @c_113= (SELECT id FROM sys_class WHERE class_name='大一计算机科学' AND status=1);
SET @c_122= (SELECT id FROM sys_class WHERE class_name='大二数学系'  AND status=1);

INSERT INTO class_member (class_id, user_id, role, join_time, status, created_at, updated_at) VALUES
-- 五年级1班: student4
(@c_51, @s4_id,  0, @now, 1, @now, @now),
-- 六年级2班: student5
(@c_62, @s5_id,  0, @now, 1, @now, @now),
-- 初一1班: student6
(@c_71, @s6_id,  0, @now, 1, @now, @now),
-- 初二1班: student7
(@c_81, @s7_id,  0, @now, 1, @now, @now),
-- 初三1班: student8
(@c_91, @s8_id,  0, @now, 1, @now, @now),
-- 高一1班: student9
(@c_101,@s9_id,  0, @now, 1, @now, @now),
-- 高二理科班: student10, student11
(@c_112,@s10_id, 0, @now, 1, @now, @now),
(@c_112,@s11_id, 0, @now, 1, @now, @now),
-- 大一计算机科学: student12, student13
(@c_113,@s12_id, 0, @now, 1, @now, @now),
(@c_113,@s13_id, 0, @now, 1, @now, @now),
-- 大二数学系: student14
(@c_122,@s14_id, 0, @now, 1, @now, @now);

-- student1 加入高三冲刺班（如果还没加入）
INSERT IGNORE INTO class_member (class_id, user_id, role, join_time, status, created_at, updated_at)
VALUES (@c_123, @s1_id, 0, @now, 1, @now, @now);

-- 同步教师角色到班级
INSERT IGNORE INTO class_member (class_id, user_id, role, join_time, status, created_at, updated_at) VALUES
(@c_51, @t3_id, 1, @now, 1, @now, @now),
(@c_62, @t3_id, 1, @now, 1, @now, @now),
(@c_71, @t2_id, 1, @now, 1, @now, @now),
(@c_81, @t2_id, 1, @now, 1, @now, @now),
(@c_91, @t2_id, 1, @now, 1, @now, @now),
(@c_101,@t1_id, 1, @now, 1, @now, @now),
(@c_112,@t1_id, 1, @now, 1, @now, @now),
(@c_123,@t1_id, 1, @now, 1, @now, @now),
(@c_113,@t4_id, 1, @now, 1, @now, @now),
(@c_122,@t4_id, 1, @now, 1, @now, @now);

-- 更新班级人数
UPDATE sys_class SET student_count = (
  SELECT COUNT(*) FROM class_member WHERE class_id=sys_class.id AND status=1 AND role=0
);

SELECT '学生加入班级 OK' AS step;

-- 同步学生年级到班级年级（学生档案与班级一致）
UPDATE sys_user u
INNER JOIN class_member cm ON cm.user_id=u.id AND cm.status=1 AND cm.role=0
INNER JOIN sys_class c ON c.id=cm.class_id AND c.status=1
SET u.grade=c.grade, u.school=c.school, u.updated_at=NOW()
WHERE c.grade IS NOT NULL AND c.grade!=''
  AND (u.grade IS NULL OR u.grade='' OR u.grade!=c.grade);

SELECT '同步年级 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 六、考试试卷 (每个班级 1-2 份)
-- ════════════════════════════════════════════════════════

INSERT INTO exam_paper (title, description, category_id, total_score, pass_score, duration,
  start_time, end_time, paper_type, shuffle_question, shuffle_option, max_attempts,
  allow_view_answer, show_score_type, anti_cheat_level, max_screen_switch,
  creator_id, status, enrolled_count, avg_score, max_score, min_score, created_at, updated_at) VALUES
-- 小学
('五年级数学期中考试', '小学五年级期中检测卷', 6, 100.00, 60.00, 60,
  '2026-03-01 08:00:00', '2026-07-30 23:59:00', 1, 1, 1, 3, 1, 1, 0, 0, @t3_id, 1, 0, 0, 0, 0, @now, @now),
('六年级语文期末检测', '小学六年级语文期末考试', 14, 100.00, 60.00, 90,
  '2026-03-15 08:00:00', '2026-07-30 23:59:00', 1, 1, 1, 1, 1, 1, 0, 0, @t3_id, 1, 0, 0, 0, 0, @now, @now),
-- 初中
('初一数学基础测试', '初一年级数学基础', 6, 100.00, 60.00, 60,
  '2026-02-20 08:00:00', '2026-07-30 23:59:00', 1, 1, 1, 2, 1, 1, 0, 0, @t2_id, 1, 0, 0, 0, 0, @now, @now),
('初二数学提高卷', '初二年级数学提高', 7, 120.00, 72.00, 90,
  '2026-03-10 08:00:00', '2026-07-30 23:59:00', 1, 1, 1, 2, 1, 1, 0, 0, @t2_id, 1, 0, 0, 0, 0, @now, @now),
('初三中考数学模拟', '中考数学全真模拟', 6, 150.00, 90.00, 120,
  '2026-04-01 08:00:00', '2026-07-01 23:59:00', 2, 1, 1, 1, 1, 1, 1, 3, @t2_id, 1, 0, 0, 0, 0, @now, @now),
-- 高中
('高一数学月考', '高一上学期月考', 7, 150.00, 90.00, 120,
  '2026-03-05 08:00:00', '2026-06-30 23:59:00', 1, 1, 1, 2, 1, 1, 1, 2, @t1_id, 1, 0, 0, 0, 0, @now, @now),
('高二理科综合测试', '理化生综合', 6, 300.00, 180.00, 150,
  '2026-04-15 08:00:00', '2026-06-30 23:59:00', 2, 1, 1, 1, 1, 1, 2, 5, @t1_id, 1, 0, 0, 0, 0, @now, @now),
('高三冲刺模拟一', '高考冲刺全真模拟', 6, 150.00, 90.00, 120,
  '2026-05-01 08:00:00', '2026-06-05 23:59:00', 2, 0, 0, 1, 1, 2, 2, 5, @t1_id, 1, 0, 0, 0, 0, @now, @now),
-- 大学
('大学JAVA基础', 'JAVA编程语言基础', 18, 100.00, 60.00, 90,
  '2026-03-20 08:00:00', '2026-07-15 23:59:00', 1, 1, 1, 2, 1, 1, 0, 0, @t4_id, 1, 0, 0, 0, 0, @now, @now);

-- 试卷ID变量
SET @ep1 = (SELECT id FROM exam_paper WHERE title='五年级数学期中考试' LIMIT 1);
SET @ep2 = (SELECT id FROM exam_paper WHERE title='六年级语文期末检测' LIMIT 1);
SET @ep3 = (SELECT id FROM exam_paper WHERE title='初一数学基础测试' LIMIT 1);
SET @ep4 = (SELECT id FROM exam_paper WHERE title='初二数学提高卷' LIMIT 1);
SET @ep5 = (SELECT id FROM exam_paper WHERE title='初三中考数学模拟' LIMIT 1);
SET @ep6 = (SELECT id FROM exam_paper WHERE title='高一数学月考' LIMIT 1);
SET @ep7 = (SELECT id FROM exam_paper WHERE title='高二理科综合测试' LIMIT 1);
SET @ep8 = (SELECT id FROM exam_paper WHERE title='高三冲刺模拟一' LIMIT 1);
SET @ep9 = (SELECT id FROM exam_paper WHERE title='大学JAVA基础' LIMIT 1);

-- 为试卷分配题目
INSERT INTO exam_paper_question (paper_id, question_id, sort, score) VALUES
-- 五年级数学: 1,2,3,4 (单选×3 + 判断×1)
(@ep1, 1, 1, 20), (@ep1, 2, 2, 20), (@ep1, 3, 3, 20), (@ep1, 4, 4, 40),
-- 六年级语文: 15-24 全部语文题
(@ep2, 15, 1, 10), (@ep2, 16, 2, 10), (@ep2, 17, 3, 10), (@ep2, 18, 4, 10),
(@ep2, 19, 5, 10), (@ep2, 20, 6, 10), (@ep2, 21, 7, 10), (@ep2, 22, 8, 10),
(@ep2, 23, 9, 10), (@ep2, 24, 10, 10),
-- 初一数学: 1,2,3,5,7
(@ep3, 1, 1, 20), (@ep3, 2, 2, 20), (@ep3, 3, 3, 20), (@ep3, 5, 4, 20), (@ep3, 7, 5, 20),
-- 初二数学: 4,5,8,9,10
(@ep4, 4, 1, 24), (@ep4, 5, 2, 24), (@ep4, 8, 3, 24), (@ep4, 9, 4, 24), (@ep4, 10, 5, 24),
-- 初三中考: 1,2,4,5
(@ep5, 1, 1, 35), (@ep5, 2, 2, 35), (@ep5, 4, 3, 40), (@ep5, 5, 4, 40),
-- 高一数学: 4,5,7,8,9,10
(@ep6, 4, 1, 25), (@ep6, 5, 2, 25), (@ep6, 7, 3, 25), (@ep6, 8, 4, 25), (@ep6, 9, 5, 25), (@ep6, 10, 6, 25),
-- 高二理综: 1,4,5,7
(@ep7, 1, 1, 75), (@ep7, 4, 2, 75), (@ep7, 5, 3, 75), (@ep7, 7, 4, 75),
-- 高三冲刺: 1,2,4,5
(@ep8, 1, 1, 35), (@ep8, 2, 2, 35), (@ep8, 4, 3, 40), (@ep8, 5, 4, 40),
-- 大学JAVA: 25
(@ep9, 25, 1, 100);

SELECT '试卷题目 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 七、考试记录
-- ════════════════════════════════════════════════════════

INSERT INTO exam_session (paper_id, user_id, class_id, start_time, submit_time, duration_used,
  status, total_score, objective_score, subjective_score, created_at, updated_at) VALUES
-- 小学
(@ep1, @s4_id, @c_51, '2026-04-10 09:00:00', '2026-04-10 10:00:00', 55, 2, 80.00, 60.00, 20.00, @now, @now),
(@ep2, @s5_id, @c_62, '2026-04-12 09:00:00', '2026-04-12 10:28:00', 85, 2, 72.00, 50.00, 22.00, @now, @now),
-- 初中（含不完整的答题状态）
(@ep3, @s6_id, @c_71, '2026-03-25 14:00:00', '2026-03-25 15:00:00', 58, 2, 65.00, 40.00, 25.00, @now, @now),
(@ep4, @s7_id, @c_81, '2026-04-01 10:00:00', '2026-04-01 11:20:00', 75, 2, 78.00, 48.00, 30.00, @now, @now),
(@ep5, @s8_id, @c_91, '2026-04-15 08:00:00', '2026-04-15 09:55:00', 110, 2, 102.00, 70.00, 32.00, @now, @now),
-- 高中
(@ep6, @s9_id, @c_101,'2026-04-08 09:00:00', '2026-04-08 10:50:00', 105, 2, 88.00, 60.00, 28.00, @now, @now),
(@ep7, @s10_id,@c_112,'2026-05-10 08:00:00', '2026-05-10 10:20:00', 135, 2, 215.00, 150.00, 65.00, @now, @now),
(@ep7, @s11_id,@c_112,'2026-05-10 08:00:00', '2026-05-10 10:25:00', 140, 2, 198.00, 130.00, 68.00, @now, @now),
(@ep8, @s1_id, @c_123,'2026-05-15 08:00:00', '2026-05-15 09:50:00', 108, 2, 125.00, 90.00, 35.00, @now, @now),
-- 大学
(@ep9, @s12_id,@c_113,'2026-04-20 10:00:00', '2026-04-20 11:20:00', 78, 2, 75.00, 75.00, 0.00, @now, @now),
(@ep9, @s13_id,@c_113,'2026-04-20 10:00:00', '2026-04-20 11:15:00', 72, 2, 82.00, 82.00, 0.00, @now, @now),
-- 进行中状态（未提交）
(@ep9, @s14_id,@c_122,'2026-04-25 09:00:00', NULL, 0, 1, 0.00, 0.00, 0.00, @now, @now);

UPDATE exam_paper SET enrolled_count = (SELECT COUNT(DISTINCT user_id) FROM exam_session WHERE paper_id=id);

SELECT '考试记录 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 八、作答记录（部分代表性）
-- ════════════════════════════════════════════════════════

SET @ses1 = (SELECT id FROM exam_session WHERE user_id=@s4_id LIMIT 1);
SET @ses3 = (SELECT id FROM exam_session WHERE user_id=@s6_id LIMIT 1);
SET @ses6 = (SELECT id FROM exam_session WHERE user_id=@s9_id LIMIT 1);
SET @ses8 = (SELECT id FROM exam_session WHERE user_id=@s1_id LIMIT 1);

INSERT INTO exam_answer (session_id, question_id, user_answer, is_correct, score, created_at, updated_at) VALUES
-- student4 五年级数学: 对3错1
(@ses1, 1, 'B', 1, 20, @now, @now),
(@ses1, 2, 'C', 1, 20, @now, @now),
(@ses1, 3, '正确', 1, 20, @now, @now),
(@ses1, 4, 'C', 0, 20, @now, @now),
-- student6 初一数学: 对3错2
(@ses3, 1, 'B', 1, 20, @now, @now),
(@ses3, 2, 'C', 1, 20, @now, @now),
(@ses3, 3, '错误', 0, 0, @now, @now),
(@ses3, 5, '21', 1, 20, @now, @now),
(@ses3, 7, 'A', 0, 0, @now, @now),
-- student9 高一数学: 对3错1
(@ses6, 4, 'C', 1, 25, @now, @now),
(@ses6, 5, '21', 1, 25, @now, @now),
(@ses6, 7, 'A', 0, 0, @now, @now),
(@ses6, 8, 'A', 1, 25, @now, @now),
-- student1 高三冲刺: 全对
(@ses8, 1, 'B', 1, 35, @now, @now),
(@ses8, 2, 'C', 1, 35, @now, @now),
(@ses8, 4, 'C', 1, 40, @now, @now),
(@ses8, 5, '21', 1, 40, @now, @now);

SELECT '作答记录 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 九、组队PK
-- ════════════════════════════════════════════════════════

INSERT INTO pk_team (team_name, slogan, max_members, current_members, captain_id, join_type, total_score, win_count, lose_count, status, created_at, updated_at) VALUES
('尖峰队',   '勇攀高峰，永不言败', 5, 2, @s1_id, 1, 125, 2, 0, 1, @now, @now),
('追风少年', '青春无悔，追风逐梦', 5, 2, @s6_id, 2, 65,  1, 1, 1, @now, @now),
('学霸联盟', '知识就是力量',     5, 2, @s8_id, 2, 102, 1, 1, 1, @now, @now);

SET @t1 = (SELECT id FROM pk_team WHERE team_name='尖峰队');
SET @t2 = (SELECT id FROM pk_team WHERE team_name='追风少年');
SET @t3 = (SELECT id FROM pk_team WHERE team_name='学霸联盟');

INSERT INTO pk_team_member (team_id, user_id, role, join_time, status, created_at, updated_at) VALUES
(@t1, @s1_id, 1, @now, 1, @now, @now),
(@t1, @s5_id, 0, @now, 1, @now, @now),
(@t2, @s6_id, 1, @now, 1, @now, @now),
(@t2, @s7_id, 0, @now, 1, @now, @now),
(@t3, @s8_id, 1, @now, 1, @now, @now),
(@t3, @s9_id, 0, @now, 1, @now, @now);

INSERT INTO pk_match (match_no, match_type, team_a_id, team_b_id, paper_id, status,
  start_time, end_time, winner_team_id, total_rounds, current_round) VALUES
('PK20260401001', 1, @t1, @t2, @ep3, 2, '2026-04-01 10:00:00', '2026-04-01 10:40:00', @t1, 5, 5),
('PK20260415001', 1, @t1, @t3, @ep5, 2, '2026-04-15 14:00:00', '2026-04-15 14:50:00', @t1, 5, 5),
('PK20260501001', 1, @t2, @t3, @ep3, 2, '2026-05-01 09:00:00', '2026-05-01 09:35:00', @t3, 5, 5);

SELECT 'PK数据 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 十、学习小组
-- ════════════════════════════════════════════════════════

INSERT INTO study_group (group_name, description, tags, join_type, max_members, current_members,
  creator_id, level, active_score, status, created_at, updated_at) VALUES
('数学爱好者',     '一起刷题攻克数学',       '数学,刷题',       1, 50, 1, @s1_id, 3, 150, 1, @now, @now),
('英语角',         '每日英语听说读写',       '英语,口语',       1, 50, 1, @s5_id, 2, 80,  1, @now, @now),
('编程入门',       '从零开始学编程',        '编程,JAVA,Python', 1, 100, 1, @s12_id,1, 200, 1, @now, @now),
('高考冲刺互助组', '高考复习资料分享与互助', '高考,复习',       2, 30, 1, @s1_id, 3, 60,  1, @now, @now),
('大学考研交流',   '考研资料共享交流',      '考研,数学,英语',  1, 60, 1, @s14_id,2, 35,  1, @now, @now);

SET @g1 = (SELECT id FROM study_group WHERE group_name='数学爱好者');
SET @g2 = (SELECT id FROM study_group WHERE group_name='英语角');
SET @g3 = (SELECT id FROM study_group WHERE group_name='编程入门');
SET @g4 = (SELECT id FROM study_group WHERE group_name='高考冲刺互助组');
SET @g5 = (SELECT id FROM study_group WHERE group_name='大学考研交流');

INSERT INTO study_group_member (group_id, user_id, role, contribution, join_time, status, created_at, updated_at) VALUES
(@g1, @s1_id, 1, 120, @now, 1, @now, @now),
(@g1, @s6_id, 0, 30,  @now, 1, @now, @now),
(@g1, @s7_id, 0, 20,  @now, 1, @now, @now),
(@g2, @s5_id, 1, 60,  @now, 1, @now, @now),
(@g2, @s9_id, 0, 20,  @now, 1, @now, @now),
(@g2, @s4_id, 0, 10,  @now, 1, @now, @now),
(@g3, @s12_id,1, 150, @now, 1, @now, @now),
(@g3, @s13_id,0, 50,  @now, 1, @now, @now),
(@g3, @s8_id, 0, 10,  @now, 1, @now, @now),
(@g4, @s1_id, 1, 40,  @now, 1, @now, @now),
(@g4, @s9_id, 0, 10,  @now, 1, @now, @now),
(@g4, @s10_id,0, 5,   @now, 1, @now, @now),
(@g4, @s11_id,0, 5,   @now, 1, @now, @now),
(@g5, @s14_id,1, 25,  @now, 1, @now, @now),
(@g5, @s12_id,0, 10,  @now, 1, @now, @now);

-- 更新小组成员人数
UPDATE study_group SET current_members = (
  SELECT COUNT(*) FROM study_group_member WHERE group_id=study_group.id AND status=1
);

SELECT '学习小组 OK' AS step;

-- ════════════════════════════════════════════════════════
-- 十一、数据统计验证
-- ════════════════════════════════════════════════════════

SELECT '                数据统计                ' AS '';
SELECT '─────────────────────────────────────────' AS '';
SELECT '教师数' AS category, COUNT(*) AS cnt FROM sys_user u
  INNER JOIN sys_user_role ur ON ur.user_id=u.id
  INNER JOIN sys_role r ON r.id=ur.role_id
  WHERE r.role_code='TEACHER' AND u.deleted=0
UNION ALL
SELECT '学生数', COUNT(*) FROM sys_user u
  INNER JOIN sys_user_role ur ON ur.user_id=u.id
  INNER JOIN sys_role r ON r.id=ur.role_id
  WHERE r.role_code='STUDENT' AND u.deleted=0
UNION ALL
SELECT '班级数', COUNT(*) FROM sys_class WHERE status=1
UNION ALL
SELECT '班级成员数', COUNT(*) FROM class_member WHERE status=1
UNION ALL
SELECT '试卷数', COUNT(*) FROM exam_paper
UNION ALL
SELECT '考试记录数', COUNT(*) FROM exam_session
UNION ALL
SELECT '作答记录数', COUNT(*) FROM exam_answer
UNION ALL
SELECT 'PK队伍数', COUNT(*) FROM pk_team
UNION ALL
SELECT 'PK队伍成员数', COUNT(*) FROM pk_team_member WHERE status=1
UNION ALL
SELECT 'PK比赛数', COUNT(*) FROM pk_match
UNION ALL
SELECT '学习小组数', COUNT(*) FROM study_group
UNION ALL
SELECT '小组成员数', COUNT(*) FROM study_group_member WHERE status=1;

-- 未加入班级的学生（测试用）
SELECT '──── 未加入班级的学生（测试数据）────' AS '';
SELECT u.id, u.username, u.nickname, u.grade
FROM sys_user u
INNER JOIN sys_user_role ur ON ur.user_id=u.id
INNER JOIN sys_role r ON r.id=ur.role_id AND r.role_code='STUDENT'
WHERE u.deleted=0
  AND NOT EXISTS (SELECT 1 FROM class_member cm WHERE cm.user_id=u.id AND cm.status=1);
