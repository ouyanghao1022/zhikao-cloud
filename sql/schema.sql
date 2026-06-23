-- =====================================================
-- 智考云 - 在线考试与学习平台 数据库脚本
-- MySQL 8.0+
-- =====================================================
CREATE Database zhikao_cloud;
use zhikao_cloud;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 删除已有表（按依赖逆序）
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_notification_setting;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS study_group_resource;
DROP TABLE IF EXISTS study_group_task;
DROP TABLE IF EXISTS study_group_chat;
DROP TABLE IF EXISTS study_group_member;
DROP TABLE IF EXISTS study_group;
DROP TABLE IF EXISTS leaderboard_record;
DROP TABLE IF EXISTS leaderboard;
DROP TABLE IF EXISTS pk_match_record;
DROP TABLE IF EXISTS pk_match;
DROP TABLE IF EXISTS pk_team_member;
DROP TABLE IF EXISTS pk_team;
DROP TABLE IF EXISTS discussion_like;
DROP TABLE IF EXISTS discussion_follow;
DROP TABLE IF EXISTS discussion_comment;
DROP TABLE IF EXISTS discussion_post;
DROP TABLE IF EXISTS discussion_section;
DROP TABLE IF EXISTS wrong_review_plan;
DROP TABLE IF EXISTS wrong_note_tag;
DROP TABLE IF EXISTS wrong_notebook;
DROP TABLE IF EXISTS favorite_item;
DROP TABLE IF EXISTS favorite_folder;
DROP TABLE IF EXISTS exam_certificate;
DROP TABLE IF EXISTS exam_monitor_log;
DROP TABLE IF EXISTS exam_answer;
DROP TABLE IF EXISTS exam_record;
DROP TABLE IF EXISTS exam_session;
DROP TABLE IF EXISTS exam_paper_question;
DROP TABLE IF EXISTS exam_paper;
DROP TABLE IF EXISTS question_favorite;
DROP TABLE IF EXISTS question_tag_rel;
DROP TABLE IF EXISTS question_tag;
DROP TABLE IF EXISTS question_option;
DROP TABLE IF EXISTS question_analysis;
DROP TABLE IF EXISTS question_bank;
DROP TABLE IF EXISTS question_category;
DROP TABLE IF EXISTS user_integral_log;
DROP TABLE IF EXISTS user_level;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

-- =====================================================
-- 1. 系统用户与权限模块
-- =====================================================

-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(加密)',
    nickname VARCHAR(50) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    school VARCHAR(100) COMMENT '学校',
    grade VARCHAR(20) COMMENT '年级',
    signature VARCHAR(200) COMMENT '个性签名',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1正常',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    open_id VARCHAR(100) COMMENT '第三方OpenID',
    oauth_type VARCHAR(20) COMMENT '第三方类型：wechat/qq/dingtalk',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码：SUPER_ADMIN/TEACHER/STUDENT/GUEST',
    description VARCHAR(200) COMMENT '角色描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 菜单/权限表
CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_type TINYINT NOT NULL COMMENT '类型：0目录 1菜单 2按钮',
    path VARCHAR(200) COMMENT '路由路径',
    component VARCHAR(200) COMMENT '组件路径',
    perms VARCHAR(100) COMMENT '权限标识',
    icon VARCHAR(50) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限表';

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- =====================================================
-- 2. 用户扩展信息模块
-- =====================================================

-- 用户资料表
CREATE TABLE user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    total_exams INT DEFAULT 0 COMMENT '总考试次数',
    avg_score DECIMAL(5,2) DEFAULT 0 COMMENT '平均分',
    win_rate DECIMAL(5,2) DEFAULT 0 COMMENT '胜率(%)',
    total_badges INT DEFAULT 0 COMMENT '获得徽章数',
    level TINYINT DEFAULT 1 COMMENT '等级：1-青铜 2-白银 3-黄金 4-铂金 5-钻石 6-星耀 7-王者',
    experience INT DEFAULT 0 COMMENT '经验值',
    integration INT DEFAULT 0 COMMENT '积分',
    continuous_sign_days INT DEFAULT 0 COMMENT '连续签到天数',
    last_sign_date DATE COMMENT '最后签到日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料扩展表';

-- 用户积分日志表
CREATE TABLE user_integral_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    change_type TINYINT NOT NULL COMMENT '变动类型：1考试得分 2每日签到 3连续打卡 4分享 5邀请好友 6消费',
    change_value INT NOT NULL COMMENT '变动值(正增负减)',
    current_value INT NOT NULL COMMENT '变动后积分',
    description VARCHAR(200) COMMENT '描述',
    related_id BIGINT COMMENT '关联ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分变动日志表';

-- 用户等级配置表
CREATE TABLE user_level (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    level_name VARCHAR(20) NOT NULL COMMENT '等级名称：青铜/白银/黄金/铂金/钻石/星耀/王者',
    level_code TINYINT NOT NULL UNIQUE COMMENT '等级编码：1-7',
    min_experience INT NOT NULL COMMENT '最小经验值',
    max_experience INT NOT NULL COMMENT '最大经验值',
    icon VARCHAR(255) COMMENT '等级图标',
    privileges TEXT COMMENT '等级特权JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户等级配置表';

-- =====================================================
-- 3. 题库模块
-- =====================================================

-- 题库分类表（学科→章节→知识点 多级）
CREATE TABLE question_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_type TINYINT DEFAULT 1 COMMENT '类型：1学科 2章节 3知识点',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1正常',
    allow_practice TINYINT DEFAULT 1 COMMENT '是否允许练习：0否 1是',
    creator_id BIGINT COMMENT '创建者ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库分类表（学科/章节/知识点）';

-- 题库表
CREATE TABLE question_bank (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    category_id BIGINT NOT NULL COMMENT '所属分类ID',
    question_type TINYINT NOT NULL COMMENT '题型：1单选 2多选 3判断 4填空 5简答 6论述 7编程 8组合题',
    difficulty TINYINT DEFAULT 1 COMMENT '难度：1简单 2中等 3困难 4极难',
    title TEXT NOT NULL COMMENT '题目标题/题干',
    content TEXT COMMENT '题目详情（材料分析题材料）',
    answer TEXT COMMENT '正确答案',
    answer_analysis TEXT COMMENT '解析内容',
    video_analysis_url VARCHAR(255) COMMENT '视频解析链接',
    is_past_exam TINYINT DEFAULT 0 COMMENT '是否历年真题：0否 1是',
    source VARCHAR(100) COMMENT '题目来源',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    correct_rate DECIMAL(5,2) DEFAULT 0 COMMENT '正确率(%)',
    score DECIMAL(5,2) DEFAULT 1.00 COMMENT '题目分值',
    creator_id BIGINT COMMENT '创建人ID',
    bank_type TINYINT DEFAULT 1 COMMENT '题库类型：1公开 2私有 3机构共享',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1正常',
    allow_practice TINYINT DEFAULT 1 COMMENT '是否允许练习：0否 1是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_id (category_id),
    INDEX idx_question_type (question_type),
    INDEX idx_difficulty (difficulty),
    INDEX idx_bank_type (bank_type),
    FULLTEXT idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 题目选项表（单选/多选/判断）
CREATE TABLE question_option (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选项ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    option_label VARCHAR(10) NOT NULL COMMENT '选项标签：A/B/C/D',
    option_content TEXT NOT NULL COMMENT '选项内容',
    is_correct TINYINT DEFAULT 0 COMMENT '是否为正确答案：0否 1是',
    sort INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

-- 题目解析表
CREATE TABLE question_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    question_id BIGINT NOT NULL UNIQUE COMMENT '题目ID',
    text_analysis TEXT COMMENT '文字解析',
    video_url VARCHAR(255) COMMENT '视频解析链接',
    knowledge_points VARCHAR(500) COMMENT '涉及知识点，逗号分隔',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目解析表';

-- 题目标签表
CREATE TABLE question_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    tag_name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    tag_color VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目标签表';

-- 题目标签关联表
CREATE TABLE question_tag_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_question_tag (question_id, tag_id),
    INDEX idx_question_id (question_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目标签关联表';

-- 题目收藏表
CREATE TABLE question_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    folder_id BIGINT COMMENT '收藏夹ID',
    note VARCHAR(500) COMMENT '收藏笔记',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_user_id (user_id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目收藏表';

-- =====================================================
-- 4. 考试模块
-- =====================================================

-- 试卷表
CREATE TABLE exam_paper (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '试卷ID',
    title VARCHAR(200) NOT NULL COMMENT '试卷标题',
    description TEXT COMMENT '试卷描述',
    category_id BIGINT COMMENT '分类ID',
    total_score DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '总分',
    pass_score DECIMAL(5,2) COMMENT '及格分',
    duration INT COMMENT '考试时长(分钟)',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    paper_type TINYINT DEFAULT 1 COMMENT '试卷类型：1固定试卷 2随机试卷',
    shuffle_question TINYINT DEFAULT 0 COMMENT '题目乱序：0否 1是',
    shuffle_option TINYINT DEFAULT 0 COMMENT '选项乱序：0否 1是',
    max_attempts INT DEFAULT 1 COMMENT '最大作答次数',
    allow_view_answer TINYINT DEFAULT 1 COMMENT '允许查看答案：0否 1是',
    show_score_type TINYINT DEFAULT 1 COMMENT '成绩显示策略：1交卷即显示 2统一时间显示 3不显示',
    anti_cheat_level TINYINT DEFAULT 0 COMMENT '防作弊等级：0无 1切屏检测 2随机拍照 3IP限制 4设备绑定',
    max_screen_switch INT DEFAULT 0 COMMENT '最大切屏次数(0不限制)',
    creator_id BIGINT COMMENT '创建人ID',
    status TINYINT DEFAULT 0 COMMENT '状态：0草稿 1发布 2已结束 3已归档',
    enrolled_count INT DEFAULT 0 COMMENT '报名/参加人数',
    avg_score DECIMAL(5,2) DEFAULT 0 COMMENT '平均分',
    max_score DECIMAL(5,2) DEFAULT 0 COMMENT '最高分',
    min_score DECIMAL(5,2) DEFAULT 0 COMMENT '最低分',
    certificate_template VARCHAR(255) COMMENT '证书模板路径',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 试卷题目关联表
CREATE TABLE exam_paper_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    sort INT DEFAULT 0 COMMENT '题目顺序',
    score DECIMAL(5,2) NOT NULL COMMENT '本题分值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_paper_question (paper_id, question_id),
    INDEX idx_paper_id (paper_id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 考试记录表（考生考试会话）
CREATE TABLE exam_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试记录ID',
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    user_id BIGINT NOT NULL COMMENT '考生ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    submit_time DATETIME COMMENT '提交时间',
    duration_used INT DEFAULT 0 COMMENT '实际用时(秒)',
    status TINYINT DEFAULT 0 COMMENT '状态：0进行中 1已提交 2已超时 3已批阅',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    device_fingerprint VARCHAR(200) COMMENT '设备指纹',
    screen_switch_count INT DEFAULT 0 COMMENT '切屏次数',
    cheat_flag TINYINT DEFAULT 0 COMMENT '作弊标记：0正常 1疑似 2确认作弊',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '总分',
    objective_score DECIMAL(5,2) DEFAULT 0 COMMENT '客观题得分',
    subjective_score DECIMAL(5,2) DEFAULT 0 COMMENT '主观题得分',
    class_id BIGINT DEFAULT NULL COMMENT '所属班级ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_paper_user (paper_id, user_id),
    INDEX idx_paper_id (paper_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- 考试-班级绑定表
CREATE TABLE IF NOT EXISTS exam_paper_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_paper_class (paper_id, class_id),
    INDEX idx_paper_id (paper_id),
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试-班级绑定表';

-- 答题记录表
CREATE TABLE exam_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    session_id BIGINT NOT NULL COMMENT '考试记录ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    user_answer TEXT COMMENT '用户答案',
    is_correct TINYINT DEFAULT 0 COMMENT '是否正确：0否 1是',
    score DECIMAL(5,2) DEFAULT 0 COMMENT '本题得分',
    corrected_by BIGINT COMMENT '批改人ID',
    corrected_at DATETIME COMMENT '批改时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_session_question (session_id, question_id),
    INDEX idx_session_id (session_id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- 考试监控日志表
CREATE TABLE exam_monitor_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    session_id BIGINT NOT NULL COMMENT '考试记录ID',
    event_type TINYINT NOT NULL COMMENT '事件类型：1切屏 2拍照 3IP变更 4设备变更 5异常答题速度',
    event_detail TEXT COMMENT '事件详情JSON',
    risk_level TINYINT DEFAULT 0 COMMENT '风险等级：0低 1中 2高',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试监控日志表';

-- 考试证书表
CREATE TABLE exam_certificate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    session_id BIGINT NOT NULL UNIQUE COMMENT '考试记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    cert_no VARCHAR(50) NOT NULL UNIQUE COMMENT '证书编号',
    cert_url VARCHAR(255) COMMENT '证书文件URL',
    issue_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '颁发时间',
    INDEX idx_user_id (user_id),
    INDEX idx_paper_id (paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试证书表';

-- =====================================================
-- 5. 错题本模块
-- =====================================================

-- 错题本表
CREATE TABLE wrong_notebook (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    exam_session_id BIGINT COMMENT '关联考试记录ID',
    wrong_answer TEXT COMMENT '错误答案',
    correct_answer TEXT COMMENT '正确答案',
    error_type TINYINT DEFAULT 0 COMMENT '错误类型：0概念不清 1粗心大意 2完全不会 3审题错误',
    master_status TINYINT DEFAULT 0 COMMENT '掌握状态：0未掌握 1模糊 2已掌握',
    review_count INT DEFAULT 0 COMMENT '复习次数',
    last_review_time DATETIME COMMENT '最后复习时间',
    next_review_time DATETIME COMMENT '下次复习时间(遗忘曲线)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_user_id (user_id),
    INDEX idx_master_status (master_status),
    INDEX idx_next_review_time (next_review_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- 错题标签表
CREATE TABLE wrong_note_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    wrong_note_id BIGINT NOT NULL COMMENT '错题ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_wrong_note_id (wrong_note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题标签表';

-- 错题复习计划表
CREATE TABLE wrong_review_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    review_date DATE NOT NULL COMMENT '复习日期',
    review_count INT DEFAULT 0 COMMENT '当日应复习错题数',
    completed_count INT DEFAULT 0 COMMENT '已完成复习数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, review_date),
    INDEX idx_review_date (review_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题复习计划表';

-- =====================================================
-- 6. 收藏夹模块
-- =====================================================

-- 收藏夹表
CREATE TABLE favorite_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    folder_name VARCHAR(100) NOT NULL COMMENT '收藏夹名称',
    description VARCHAR(200) COMMENT '描述',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开：0私有 1公开',
    sort INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏夹表';

-- 收藏项表
CREATE TABLE favorite_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    folder_id BIGINT NOT NULL COMMENT '收藏夹ID',
    item_type TINYINT NOT NULL COMMENT '收藏类型：1题目 2试卷 3学习资料 4视频链接',
    item_id BIGINT COMMENT '关联ID',
    item_title VARCHAR(200) COMMENT '标题',
    item_url VARCHAR(500) COMMENT '链接地址',
    note TEXT COMMENT '个人笔记',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_folder_id (folder_id),
    INDEX idx_item_type (item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏项表';

-- =====================================================
-- 7. 讨论区模块
-- =====================================================

-- 讨论区版块表
CREATE TABLE discussion_section (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版块ID',
    section_name VARCHAR(100) NOT NULL COMMENT '版块名称',
    description VARCHAR(200) COMMENT '版块描述',
    section_type TINYINT DEFAULT 1 COMMENT '版块类型：1学科讨论 2考试交流 3题目求助 4学习打卡 5官方公告',
    icon VARCHAR(255) COMMENT '版块图标',
    sort INT DEFAULT 0 COMMENT '排序',
    post_count INT DEFAULT 0 COMMENT '帖子数',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论区版块表';

-- 讨论区帖子表
CREATE TABLE discussion_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    section_id BIGINT NOT NULL COMMENT '版块ID',
    user_id BIGINT NOT NULL COMMENT '发帖人ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    content_type TINYINT DEFAULT 1 COMMENT '内容类型：1富文本 2Markdown',
    tags VARCHAR(200) COMMENT '话题标签，逗号分隔',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶：0否 1是',
    is_essence TINYINT DEFAULT 0 COMMENT '是否加精：0否 1是',
    is_audit_passed TINYINT DEFAULT 1 COMMENT '审核状态：0待审核 1通过 2不通过',
    audit_msg VARCHAR(200) COMMENT '审核意见',
    status TINYINT DEFAULT 1 COMMENT '状态：0删除 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_section_id (section_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_top (is_top),
    INDEX idx_created_at (created_at),
    FULLTEXT idx_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论区帖子表';

-- 讨论区评论表
CREATE TABLE discussion_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '评论人ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父评论ID(楼中楼)',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    reply_to_user_id BIGINT COMMENT '回复目标用户ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0删除 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论区评论表';

-- 讨论区点赞表
CREATE TABLE discussion_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_type TINYINT NOT NULL COMMENT '目标类型：1帖子 2评论',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target_id (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论区点赞表';

-- 讨论区关注表
CREATE TABLE discussion_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    follow_user_id BIGINT NOT NULL COMMENT '关注用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_follow (user_id, follow_user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_follow_user_id (follow_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论区关注表';

-- =====================================================
-- 8. 组队PK与排行榜模块
-- =====================================================

-- PK队伍表
CREATE TABLE pk_team (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '队伍ID',
    team_name VARCHAR(100) NOT NULL COMMENT '队名',
    slogan VARCHAR(200) COMMENT '口号',
    max_members INT DEFAULT 5 COMMENT '最大人数',
    current_members INT DEFAULT 1 COMMENT '当前人数',
    captain_id BIGINT NOT NULL COMMENT '队长ID',
    join_type TINYINT DEFAULT 1 COMMENT '加入方式：1公开 2审核 3邀请',
    total_score INT DEFAULT 0 COMMENT '队伍总积分',
    win_count INT DEFAULT 0 COMMENT '胜利次数',
    lose_count INT DEFAULT 0 COMMENT '失败次数',
    status TINYINT DEFAULT 1 COMMENT '状态：0解散 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_captain_id (captain_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK队伍表';

-- PK队伍成员表
CREATE TABLE pk_team_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    team_id BIGINT NOT NULL COMMENT '队伍ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT DEFAULT 0 COMMENT '角色：0成员 1队长',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0退出 1正常',
    INDEX idx_team_id (team_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_team_user (team_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK队伍成员表';

-- PK对战记录表
CREATE TABLE pk_match (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '对战ID',
    match_no VARCHAR(50) NOT NULL UNIQUE COMMENT '对战编号',
    match_type TINYINT NOT NULL COMMENT '对战类型：1实时1v1 23v3 35v5 4异步挑战 5排位赛',
    team_a_id BIGINT COMMENT '队伍AID',
    team_b_id BIGINT COMMENT '队伍BID',
    paper_id BIGINT COMMENT '试卷ID',
    status TINYINT DEFAULT 0 COMMENT '状态：0等待中 1进行中 2已结束',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    winner_team_id BIGINT COMMENT '获胜队伍ID',
    total_rounds INT DEFAULT 10 COMMENT '总题数/轮数',
    current_round INT DEFAULT 0 COMMENT '当前轮次',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_team_a_id (team_a_id),
    INDEX idx_team_b_id (team_b_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK对战记录表';

-- PK对战答题记录表
CREATE TABLE pk_match_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    match_id BIGINT NOT NULL COMMENT '对战ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    team_id BIGINT COMMENT '队伍ID',
    round_num INT DEFAULT 0 COMMENT '答题轮次',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    answer TEXT COMMENT '答案',
    is_correct TINYINT DEFAULT 0 COMMENT '是否正确',
    answer_time_ms INT DEFAULT 0 COMMENT '答题用时(毫秒)',
    score INT DEFAULT 0 COMMENT '本题得分(连胜加成)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_match_id (match_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK对战答题记录表';

-- 排行榜表
CREATE TABLE leaderboard (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    leaderboard_type TINYINT NOT NULL COMMENT '排行榜类型：1个人总积分 2个人胜率 3个人连胜 4战队积分 5战队胜率',
    period_type TINYINT DEFAULT 0 COMMENT '周期类型：0总榜 1周榜 2月榜 3赛季榜',
    period_key VARCHAR(20) COMMENT '周期标识：2024W25/2024M06/2024S01',
    user_id BIGINT COMMENT '用户ID(个人榜)',
    team_id BIGINT COMMENT '队伍ID(团队榜)',
    score INT DEFAULT 0 COMMENT '积分/胜率值',
    rank_num INT DEFAULT 0 COMMENT '排名',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_leaderboard_type (leaderboard_type),
    INDEX idx_period_type (period_type),
    INDEX idx_period_key (period_key),
    INDEX idx_score (score),
    INDEX idx_rank_num (rank_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜表';

-- 排行榜历史记录表
CREATE TABLE leaderboard_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    leaderboard_id BIGINT NOT NULL COMMENT '排行榜ID',
    user_id BIGINT COMMENT '用户ID',
    team_id BIGINT COMMENT '队伍ID',
    rank_num INT DEFAULT 0 COMMENT '排名',
    score INT DEFAULT 0 COMMENT '分数',
    reward_status TINYINT DEFAULT 0 COMMENT '奖励状态：0未发放 1已发放',
    season_key VARCHAR(20) COMMENT '赛季标识',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_leaderboard_id (leaderboard_id),
    INDEX idx_season_key (season_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜历史记录表';

-- =====================================================
-- 9. 学习小组模块
-- =====================================================

-- 学习小组表
CREATE TABLE study_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '小组ID',
    group_name VARCHAR(100) NOT NULL COMMENT '小组名称',
    description TEXT COMMENT '小组简介',
    tags VARCHAR(200) COMMENT '标签，逗号分隔',
    cover_url VARCHAR(255) COMMENT '封面图',
    join_type TINYINT DEFAULT 1 COMMENT '加入方式：1公开 2审核 3邀请',
    max_members INT DEFAULT 200 COMMENT '最大成员数',
    current_members INT DEFAULT 1 COMMENT '当前成员数',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    level TINYINT DEFAULT 1 COMMENT '小组等级',
    active_score INT DEFAULT 0 COMMENT '活跃度',
    status TINYINT DEFAULT 1 COMMENT '状态：0解散 1正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator_id (creator_id),
    INDEX idx_join_type (join_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习小组表';

-- 学习小组成员表
CREATE TABLE study_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    group_id BIGINT NOT NULL COMMENT '小组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT DEFAULT 0 COMMENT '角色：0成员 1管理员 2组长',
    contribution INT DEFAULT 0 COMMENT '贡献度',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0退出 1正常',
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_group_user (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习小组成员表';

-- 学习小组任务表
CREATE TABLE study_group_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    group_id BIGINT NOT NULL COMMENT '小组ID',
    creator_id BIGINT NOT NULL COMMENT '发布人ID',
    task_title VARCHAR(200) NOT NULL COMMENT '任务标题',
    task_content TEXT COMMENT '任务内容',
    deadline DATETIME COMMENT '截止时间',
    completed_count INT DEFAULT 0 COMMENT '完成人数',
    status TINYINT DEFAULT 1 COMMENT '状态：0已取消 1进行中 2已结束',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习小组任务表';

-- 学习小组聊天记录表
CREATE TABLE study_group_chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    group_id BIGINT NOT NULL COMMENT '小组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '消息内容',
    content_type TINYINT DEFAULT 1 COMMENT '内容类型：1文本 2图片 3文件',
    file_url VARCHAR(255) COMMENT '文件URL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习小组聊天记录表';

-- 学习小组资源表
CREATE TABLE study_group_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    group_id BIGINT NOT NULL COMMENT '小组ID',
    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    file_url VARCHAR(255) NOT NULL COMMENT '文件URL',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习小组资源表';

-- =====================================================
-- 10. 消息通知模块
-- =====================================================

-- 系统消息表
CREATE TABLE sys_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    sender_id BIGINT COMMENT '发送人ID(系统消息为NULL)',
    message_type TINYINT NOT NULL COMMENT '消息类型：1系统通知 2考试提醒 3组队邀请 4@提醒 5回复提醒 6PK邀请',
    title VARCHAR(100) COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    related_id BIGINT COMMENT '关联ID',
    related_url VARCHAR(255) COMMENT '关联链接',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息表';

-- 消息通知设置表
CREATE TABLE sys_notification_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    system_notice TINYINT DEFAULT 1 COMMENT '系统通知：0关闭 1开启',
    exam_remind TINYINT DEFAULT 1 COMMENT '考试提醒',
    team_invite TINYINT DEFAULT 1 COMMENT '组队邀请',
    at_remind TINYINT DEFAULT 1 COMMENT '@提醒',
    reply_remind TINYINT DEFAULT 1 COMMENT '回复提醒',
    pk_invite TINYINT DEFAULT 1 COMMENT 'PK邀请',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知设置表';

-- =====================================================
-- 11. 系统日志模块
-- =====================================================

-- 操作日志表
CREATE TABLE sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(200) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(50) COMMENT 'IP地址',
    status TINYINT DEFAULT 1 COMMENT '状态：0失败 1成功',
    error_msg TEXT COMMENT '错误信息',
    duration INT DEFAULT 0 COMMENT '耗时(毫秒)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 登录日志表
CREATE TABLE sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ip VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(100) COMMENT '登录地点',
    browser VARCHAR(50) COMMENT '浏览器',
    os VARCHAR(50) COMMENT '操作系统',
    status TINYINT DEFAULT 1 COMMENT '状态：0失败 1成功',
    msg VARCHAR(200) COMMENT '登录信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 用户积分表（签到、挑战等积分系统）
CREATE TABLE IF NOT EXISTS user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_points INT DEFAULT 0 COMMENT '总积分',
    current_level INT DEFAULT 1 COMMENT '当前等级',
    experience INT DEFAULT 0 COMMENT '经验值',
    checkin_streak INT DEFAULT 0 COMMENT '连续签到天数',
    last_checkin_date DATE COMMENT '最后签到日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分表';

-- 插入超级管理员角色
INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '系统最高权限'),
('教师/出题人', 'TEACHER', '题库管理、试卷组卷、考试发布、成绩批改'),
('学生', 'STUDENT', '参加考试、错题本、讨论区、组队PK'),
('访客', 'GUEST', '浏览公开题库、查看排行榜');

-- 插入超级管理员用户（密码：admin123，BCrypt加密）
INSERT INTO sys_user (username, password, nickname, email, phone, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '超级管理员', 'admin@zhikao.com', '13800138000', 1);

-- 关联管理员用户与角色
INSERT INTO sys_user_role (user_id, role_id) SELECT 1, id FROM sys_role WHERE role_code = 'SUPER_ADMIN';

-- 插入用户等级配置
INSERT INTO user_level (level_name, level_code, min_experience, max_experience) VALUES
('青铜', 1, 0, 999),
('白银', 2, 1000, 2999),
('黄金', 3, 3000, 5999),
('铂金', 4, 6000, 9999),
('钻石', 5, 10000, 14999),
('星耀', 6, 15000, 24999),
('王者', 7, 25000, 999999);

-- 插入讨论区默认版块
INSERT INTO discussion_section (section_name, description, section_type) VALUES
('数学讨论区', '数学相关学习讨论', 1),
('英语讨论区', '英语学习交流', 1),
('物理讨论区', '物理学习讨论', 1),
('考试交流区', '备考策略、经验分享', 2),
('题目求助区', '疑难题目互助解答', 3),
('学习打卡区', '每日学习记录，互相监督', 4),
('官方公告区', '平台官方通知公告', 5);

-- 插入题库顶级分类
INSERT INTO question_category (parent_id, category_name, category_type) VALUES
(0, '数学', 1),
(0, '英语', 1),
(0, '物理', 1),
(0, '化学', 1),
(0, '语文', 1);

-- 插入题库二级分类（A/B题库）
INSERT INTO question_category (parent_id, category_name, category_type, sort) VALUES
(1, '数学-A题库', 2, 1),
(1, '数学-B题库', 2, 2),
(2, '英语-A题库', 2, 1),
(2, '英语-B题库', 2, 2),
(3, '物理-A题库', 2, 1),
(3, '物理-B题库', 2, 2),
(4, '化学-A题库', 2, 1),
(4, '化学-B题库', 2, 2),
(5, '语文-A题库', 2, 1),
(5, '语文-B题库', 2, 2);

SET FOREIGN_KEY_CHECKS = 1;
