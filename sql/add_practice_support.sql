-- ============================================================
-- 题库练习模式支持 — 添加 allow_practice 字段
-- 执行时间：2026-06-23
-- ============================================================

-- 1. question_bank 表添加 allow_practice 列
ALTER TABLE question_bank 
  ADD COLUMN allow_practice TINYINT DEFAULT 1 COMMENT '是否允许练习：0否 1是' AFTER status;

-- 2. question_category 表添加 allow_practice 列
ALTER TABLE question_category 
  ADD COLUMN allow_practice TINYINT DEFAULT 1 COMMENT '是否允许练习：0否 1是' AFTER status;

-- 3. question_category 表添加 creator_id 列（教师隔离）
ALTER TABLE question_category 
  ADD COLUMN creator_id BIGINT COMMENT '创建者ID' AFTER allow_practice;

-- 4. 把现有题目的 allow_practice 设为 1（允许练习）
UPDATE question_bank SET allow_practice = 1 WHERE allow_practice IS NULL;

-- 5. 把现有分类的 allow_practice 设为 1（允许练习）
UPDATE question_category SET allow_practice = 1 WHERE allow_practice IS NULL;
