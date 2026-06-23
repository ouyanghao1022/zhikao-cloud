-- 讨论区审核功能字段（如果数据库中尚不存在则执行）
-- 检查方法：SHOW COLUMNS FROM discussion_post LIKE 'is_audit_passed';
-- 如果返回空则执行以下语句：

ALTER TABLE discussion_post
  ADD COLUMN is_audit_passed TINYINT DEFAULT 1 COMMENT '审核状态：0待审核 1通过 2不通过',
  ADD COLUMN audit_msg VARCHAR(200) COMMENT '审核意见';

-- 更新现有帖子为已通过状态
UPDATE discussion_post SET is_audit_passed = 1 WHERE is_audit_passed IS NULL;
