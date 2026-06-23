-- =====================================================
-- 智考云 - 测试数据脚本
-- 运行前确保已执行 schema.sql
-- =====================================================
USE zhikao_cloud;
SET NAMES utf8mb4;

-- =====================================================
-- 班级成员表（如果尚未创建）
-- =====================================================
CREATE TABLE IF NOT EXISTS class_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT DEFAULT 0 COMMENT '角色：0学生 1教师(创建者)',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0已退出 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_class_id (class_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_class_user (class_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级成员表';

-- =====================================================
-- 1. 插入教师和学生的测试用户
-- =====================================================

-- 密码均为对应账号名加"123"的BCrypt加密（与admin123使用相同hash）
INSERT INTO sys_user (username, password, nickname, email, school, grade, status) VALUES
('teacher1', '$2a$10$sjkGxAbcGPokQKMCmH4wjuFtnJisG/EDmcrX4Gp0C9bj8kFRQ0mRK', '张老师', 'teacher1@zhikao.com', '第一中学', NULL, 1),
('student1', '$2a$10$sjkGxAbcGPokQKMCmH4wjuFtnJisG/EDmcrX4Gp0C9bj8kFRQ0mRK', '李明', 'student1@zhikao.com', '第一中学', '高二', 1);

-- 为用户分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(2, 2),  -- teacher1 -> TEACHER
(3, 3);  -- student1 -> STUDENT

-- 为用户创建资料扩展记录
INSERT INTO user_profile (user_id, total_exams, avg_score, level, experience, integration) VALUES
(2, 0, 0, 1, 0, 0),
(3, 8, 82.50, 3, 4500, 1200);

-- =====================================================
-- 2. 插入班级测试数据
-- =====================================================

INSERT INTO sys_class (class_name, class_code, teacher_id, school, grade, description, max_students, student_count, status, created_at, updated_at) VALUES
('高二3班', 'AB3C9K', 2, '第一中学', '高二', '高二三班数学学习交流群', 50, 2, 1, NOW(), NOW());

-- 班级成员
INSERT INTO class_member (class_id, user_id, role, join_time, status, created_at, updated_at) VALUES
(1, 2, 1, NOW(), 1, NOW(), NOW()),  -- 张老师（创建者/教师）
(1, 3, 0, NOW(), 1, NOW(), NOW());  -- 李明（学生）

-- =====================================================
-- 3. 插入题库测试数据（数学题）
-- =====================================================

-- 单选题 1
INSERT INTO question_bank (category_id, question_type, difficulty, title, content, answer, answer_analysis, score, creator_id, bank_type, status) VALUES
(1, 1, 1, '下列哪个数是质数？', NULL, 'C', '17是质数，因为它只能被1和自身整除。12、15、21都是合数。', 5.00, 2, 1, 1);

INSERT INTO question_option (question_id, option_label, option_content, is_correct, sort) VALUES
(1, 'A', '12', 0, 1),
(1, 'B', '15', 0, 2),
(1, 'C', '17', 1, 3),
(1, 'D', '21', 0, 4);

-- 单选题 2
INSERT INTO question_bank (category_id, question_type, difficulty, title, content, answer, answer_analysis, score, creator_id, bank_type, status) VALUES
(1, 1, 1, '三角形的内角和是多少度？', NULL, 'A', '三角形内角和定理：任意三角形的三个内角之和等于180°', 5.00, 2, 1, 1);

INSERT INTO question_option (question_id, option_label, option_content, is_correct, sort) VALUES
(2, 'A', '180°', 1, 1),
(2, 'B', '360°', 0, 2),
(2, 'C', '90°', 0, 3),
(2, 'D', '270°', 0, 4);

-- 判断题 3
INSERT INTO question_bank (category_id, question_type, difficulty, title, content, answer, answer_analysis, score, creator_id, bank_type, status) VALUES
(1, 3, 1, '负数没有平方根', NULL, '正确', '在实数范围内，负数没有平方根。但在复数范围内，负数有虚数平方根。', 5.00, 2, 1, 1);

-- 单选题 4（中等难度）
INSERT INTO question_bank (category_id, question_type, difficulty, title, content, answer, answer_analysis, score, creator_id, bank_type, status) VALUES
(1, 1, 2, '函数 f(x)=x²+2x+1 的最小值是多少？', NULL, 'B', 'f(x)=x²+2x+1=(x+1)²，当x=-1时，f(x)有最小值0', 10.00, 2, 1, 1);

INSERT INTO question_option (question_id, option_label, option_content, is_correct, sort) VALUES
(4, 'A', '-1', 0, 1),
(4, 'B', '0', 1, 2),
(4, 'C', '1', 0, 3),
(4, 'D', '2', 0, 4);

-- 单选题 5（困难）
INSERT INTO question_bank (category_id, question_type, difficulty, title, content, answer, answer_analysis, score, creator_id, bank_type, status) VALUES
(1, 1, 3, '已知等差数列{an}中，a₁=3，公差d=2，求a₁₀的值', NULL, 'C', '等差数列通项公式：an=a₁+(n-1)d，a₁₀=3+(10-1)×2=3+18=21', 10.00, 2, 1, 1);

INSERT INTO question_option (question_id, option_label, option_content, is_correct, sort) VALUES
(5, 'A', '19', 0, 1),
(5, 'B', '20', 0, 2),
(5, 'C', '21', 1, 3),
(5, 'D', '23', 0, 4);

-- =====================================================
-- 4. 插入试卷测试数据
-- =====================================================

-- 试卷1：数学单元测试
INSERT INTO exam_paper (title, description, category_id, total_score, pass_score, duration, start_time, end_time, paper_type, shuffle_question, shuffle_option, max_attempts, allow_view_answer, show_score_type, anti_cheat_level, max_screen_switch, creator_id, status, enrolled_count, avg_score, max_score, min_score, created_at, updated_at) VALUES
('数学单元测试（一）', '涵盖质数、三角形内角和、负数平方根等基础知识', 1, 30.00, 18.00, 45, '2026-06-01 08:00:00', '2026-07-31 23:59:59', 1, 1, 1, 3, 1, 1, 1, 3, 2, 1, 1, 25.00, 25.00, 25.00, NOW(), NOW());

-- 试卷2：数学期中模拟
INSERT INTO exam_paper (title, description, category_id, total_score, pass_score, duration, start_time, end_time, paper_type, max_attempts, allow_view_answer, show_score_type, creator_id, status, enrolled_count, avg_score, max_score, min_score, created_at, updated_at) VALUES
('高二数学期中模拟考试', '函数最值与等差数列的综合测试', 1, 100.00, 60.00, 90, '2026-06-10 08:00:00', '2026-07-31 23:59:59', 1, 2, 1, 1, 2, 1, 1, 82.50, 85.00, 80.00, NOW(), NOW());

-- 关联试卷与题目
INSERT INTO exam_paper_question (paper_id, question_id, sort, score) VALUES
(1, 1, 1, 5.00),
(1, 2, 2, 5.00),
(1, 3, 3, 5.00),
(1, 4, 4, 10.00),
(1, 5, 5, 10.00);

INSERT INTO exam_paper_question (paper_id, question_id, sort, score) VALUES
(2, 1, 1, 10.00),
(2, 2, 2, 10.00),
(2, 3, 3, 10.00),
(2, 4, 4, 20.00),
(2, 5, 5, 50.00);

-- =====================================================
-- 5. 插入考试记录（学生李明参加的两场考试）
-- =====================================================

-- 考试记录1：数学单元测试（得分25/30）
INSERT INTO exam_session (paper_id, user_id, start_time, submit_time, duration_used, status, total_score, objective_score, created_at, updated_at) VALUES
(1, 3, '2026-06-15 09:00:00', '2026-06-15 09:38:00', 2280, 3, 25.00, 25.00, NOW(), NOW());

-- 考试记录2：数学期中模拟（得分85/100）
INSERT INTO exam_session (paper_id, user_id, start_time, submit_time, duration_used, status, total_score, objective_score, created_at, updated_at) VALUES
(2, 3, '2026-06-18 14:00:00', '2026-06-18 15:25:00', 5100, 3, 85.00, 85.00, NOW(), NOW());

-- 考试答题记录（考试记录1）
INSERT INTO exam_answer (session_id, question_id, user_answer, is_correct, score, created_at, updated_at) VALUES
(1, 1, 'C', 1, 5.00, NOW(), NOW()),
(1, 2, 'A', 1, 5.00, NOW(), NOW()),
(1, 3, '正确', 1, 5.00, NOW(), NOW()),
(1, 4, 'C', 0, 0.00, NOW(), NOW()),  -- 做错了：选了C(1)，正确答案是B(0)
(1, 5, 'C', 1, 10.00, NOW(), NOW());

-- 考试答题记录（考试记录2）
INSERT INTO exam_answer (session_id, question_id, user_answer, is_correct, score, created_at, updated_at) VALUES
(2, 1, 'C', 1, 10.00, NOW(), NOW()),
(2, 2, 'A', 1, 10.00, NOW(), NOW()),
(2, 3, '错误', 0, 0.00, NOW(), NOW()),  -- 做错了：实数范围内正确但题目未指定范围
(2, 4, 'B', 1, 20.00, NOW(), NOW()),
(2, 5, 'C', 1, 45.00, NOW(), NOW());  -- 注意这里5分值为50但只答对5题的一部分，实际得分45

-- =====================================================
-- 6. 插入错题本数据
-- =====================================================

-- 错题1：问题4做错了（考试记录1）
INSERT INTO wrong_notebook (user_id, question_id, exam_session_id, wrong_answer, correct_answer, error_type, master_status, review_count, last_review_time, next_review_time, created_at, updated_at) VALUES
(3, 4, 1, 'C', 'B', 2, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NOW());

-- 错题2：问题3做错了（考试记录2）
INSERT INTO wrong_notebook (user_id, question_id, exam_session_id, wrong_answer, correct_answer, error_type, master_status, review_count, last_review_time, next_review_time, created_at, updated_at) VALUES
(3, 3, 2, '错误', '正确', 0, 0, 0, NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NOW());

-- =====================================================
-- 7. 插入讨论区帖子与评论
-- =====================================================

-- 帖子1：李明在数学讨论区发帖求助
INSERT INTO discussion_post (section_id, user_id, title, content, view_count, like_count, comment_count, status, created_at, updated_at) VALUES
(1, 3, '如何提高数学成绩？', '大家好，我是高二的学生，数学一直在80分左右徘徊，想知道有什么好的提高方法？平时也做题了，但考试总是不够理想。', 156, 12, 2, 1, NOW(), NOW());

-- 帖子2：张老师发布学习方法帖
INSERT INTO discussion_post (section_id, user_id, title, content, view_count, like_count, comment_count, is_essence, status, created_at, updated_at) VALUES
(1, 2, '高中数学学习方法的几点建议', '同学们好，我是数学张老师。这里分享几个学习建议：\n1. 基础概念要扎实，不可一知半解\n2. 做题不在多而在精，每道题要思考"为什么这样做"\n3. 整理错题本，定期复习\n4. 同类题型要归纳总结\n5. 遇到难题不要马上放弃，尝试多种思路\n\n希望对大家有帮助！', 289, 45, 1, 1, 1, NOW(), NOW());

-- 评论：李明回复张老师的帖子
INSERT INTO discussion_comment (post_id, user_id, content, like_count, status, created_at, updated_at) VALUES
(2, 3, '谢谢张老师！我会按照您说的方法去努力的。', 8, 1, NOW(), NOW());

-- 评论：张老师回复李明的求助帖
INSERT INTO discussion_comment (post_id, user_id, content, like_count, status, created_at, updated_at) VALUES
(1, 2, '建议从基础题开始练习，逐步提高难度，每天坚持做一定量的题目。同时注意整理错题，分析错误原因。', 20, 1, NOW(), NOW());

-- =====================================================
-- 8. 插入PK对战数据
-- =====================================================

-- 创建PK队伍
INSERT INTO pk_team (team_name, slogan, max_members, current_members, captain_id, join_type, total_score, win_count, lose_count, status, created_at, updated_at) VALUES
('数学冲锋队', '勇攀数学高峰！', 5, 2, 3, 1, 250, 2, 1, 1, NOW(), NOW()),
('物理小分队', '格物致知！', 5, 2, 2, 1, 180, 1, 2, 1, NOW(), NOW());

-- 队伍成员
INSERT INTO pk_team_member (team_id, user_id, role, join_time, status) VALUES
(1, 3, 1, NOW(), 1),  -- 李明是数学冲锋队队长
(1, 2, 0, NOW(), 1),  -- 张老师加入
(2, 2, 1, NOW(), 1),  -- 张老师是物理小分队队长
(2, 3, 0, NOW(), 1);  -- 李明加入

-- PK对战记录
INSERT INTO pk_match (match_no, match_type, team_a_id, team_b_id, paper_id, status, start_time, end_time, winner_team_id, total_rounds, current_round, created_at) VALUES
('PK20260615001', 1, 1, 2, 1, 2, '2026-06-15 16:00:00', '2026-06-15 16:30:00', 1, 5, 5, NOW());

-- PK对战答题记录
INSERT INTO pk_match_record (match_id, user_id, team_id, round_num, question_id, answer, is_correct, answer_time_ms, score, created_at) VALUES
(1, 3, 1, 1, 1, 'C', 1, 8500, 10, NOW()),
(1, 3, 1, 2, 2, 'A', 1, 6200, 10, NOW()),
(1, 3, 1, 3, 3, '正确', 1, 4500, 10, NOW()),
(1, 2, 2, 1, 1, 'C', 1, 9200, 8, NOW()),
(1, 2, 2, 2, 2, 'A', 1, 7500, 8, NOW()),
(1, 2, 2, 3, 4, 'A', 0, 11000, 0, NOW());

-- 排行榜数据
INSERT INTO leaderboard (leaderboard_type, period_type, user_id, score, rank_num, updated_at) VALUES
(1, 0, 3, 250, 1, NOW()),
(1, 0, 2, 180, 2, NOW()),
(2, 1, 3, 85, 1, NOW()),
(2, 1, 2, 72, 2, NOW());

-- =====================================================
-- 9. 插入学习小组数据
-- =====================================================

-- 创建学习小组
INSERT INTO study_group (group_name, description, tags, join_type, max_members, current_members, creator_id, level, active_score, status, created_at, updated_at) VALUES
('数学冲刺小组', '高二数学期末冲刺学习小组，每天打卡刷题', '数学,冲刺,高二', 1, 100, 2, 2, 3, 85, 1, NOW(), NOW());

-- 小组成员
INSERT INTO study_group_member (group_id, user_id, role, contribution, join_time, status, created_at, updated_at) VALUES
(1, 2, 2, 50, NOW(), 1, NOW(), NOW()),  -- 张老师（组长）
(1, 3, 0, 30, NOW(), 1, NOW(), NOW());  -- 李明（普通成员）

-- =====================================================
-- 完成
-- =====================================================
SELECT '测试数据插入完成！' AS message;
SELECT * FROM sys_user WHERE username IN ('teacher1', 'student1');
SELECT id, class_name, class_code FROM sys_class;
