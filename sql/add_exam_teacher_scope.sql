-- 创建考试：教师范围框定组卷
-- 持久化出题老师范围 + 随机组卷配置，支持"重新抽题"
-- 2026-06-25

ALTER TABLE exam_paper ADD COLUMN teacher_ids VARCHAR(255) NULL COMMENT '出题老师id逗号分隔，如 1,13,34';
ALTER TABLE exam_paper ADD COLUMN random_config TEXT NULL COMMENT '随机组卷配置JSON，用于重新抽题';
