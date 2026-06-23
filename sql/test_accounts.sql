-- ============================================================
-- 测试账号创建脚本
-- 密码均为 123456（BCrypt 加密）
-- ============================================================
-- 
-- BCrypt 哈希生成方式（使用项目自带的 cn.hutool）：
--   cn.hutool.crypto.digest.BCrypt.hashpw("123456", BCrypt.gensalt())
--
-- 如需重新生成密码哈希，可在项目中运行以下测试：
--   @Test
--   void genPwd() {
--       System.out.println(BCrypt.hashpw("123456", BCrypt.gensalt()));
--   }
-- ============================================================

-- 确保角色表存在
-- INSERT IGNORE 避免重复插入
INSERT IGNORE INTO sys_role (id, role_code, role_name, created_at) VALUES
(1, 'SUPER_ADMIN', '超级管理员', NOW()),
(2, 'TEACHER', '教师', NOW()),
(3, 'STUDENT', '学生', NOW())
ON DUPLICATE KEY UPDATE role_code = VALUES(role_code);

-- 创建测试学生账号：student1 / 123456
INSERT INTO sys_user (username, password, nickname, email, status, gender, created_at, updated_at)
VALUES ('student1', '$2a$10$4J1dCl/AcfNmI6mf8..UvuC0.pc7Jkto2vvkr9GOWv2jlgDFCA70K', '测试学生', 'student1@test.com', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE username = 'student1';

-- 为学生分配角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM sys_user u, sys_role r
WHERE u.username = 'student1' AND r.role_code = 'STUDENT'
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- 创建测试教师账号：teacher1 / 123456
INSERT INTO sys_user (username, password, nickname, email, status, gender, created_at, updated_at)
VALUES ('teacher1', '$2a$10$4J1dCl/AcfNmI6mf8..UvuC0.pc7Jkto2vvkr9GOWv2jlgDFCA70K', '测试教师', 'teacher1@test.com', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE username = 'teacher1';

-- 为教师分配角色
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM sys_user u, sys_role r
WHERE u.username = 'teacher1' AND r.role_code = 'TEACHER'
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- ============================================================
-- 验证查询
-- ============================================================
-- SELECT u.username, u.nickname, r.role_code
-- FROM sys_user u
-- JOIN sys_user_role ur ON u.id = ur.user_id
-- JOIN sys_role r ON ur.role_id = r.id
-- WHERE u.username IN ('student1', 'teacher1');

-- ============================================================
-- 新增字段：题目练习模式开关（已集成到 schema.sql，此处仅备忘）
-- ============================================================
-- ALTER TABLE question_bank ADD COLUMN allow_practice INT DEFAULT 1 COMMENT '是否允许练习：0否 1是' AFTER status;
