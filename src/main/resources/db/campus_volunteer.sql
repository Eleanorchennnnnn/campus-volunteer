-- =============================================================
-- 高校活动报名与志愿平台 campus_volunteer 数据库初始化脚本
-- 由黑马点评 hm-dianping 数据库改造而来
--
-- 表结构映射关系：
--   tb_user          <- tb_user           （登录标识：手机号 -> 学号，新增 role 角色）
--   tb_user_info     <- tb_user_info      （新增 total_hours 累计志愿时长）
--   tb_org           <- （全新）          社团/组织：活动的归属方、被关注对象
--   tb_activity_type <- tb_shop_type      （商户类型 -> 活动分类）
--   tb_activity      <- tb_shop + tb_voucher + tb_seckill_voucher 三表融合
--   tb_enrollment    <- tb_voucher_order  （秒杀订单 -> 报名记录，全局ID主键）
--   tb_follow        <- tb_follow         （关注用户 -> 关注组织）
--   tb_post          <- tb_blog           （探店笔记 -> 活动回顾帖/志愿心得）
--   tb_post_comment  <- tb_blog_comments
--   tb_post_like     <- blog_like         （修正 createtime 拼写为 create_time）
--
-- 二期功能不建表，全部落在 Redis：
--   现场签到 BitMap / 活动UV HyperLogLog / 组织动态Feed ZSet / 志愿时长榜 ZSet
-- =============================================================

DROP DATABASE IF EXISTS campus_volunteer;
CREATE DATABASE campus_volunteer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_volunteer;

-- -------------------------------------------------------------
-- 1. 用户表（P0）
-- -------------------------------------------------------------
CREATE TABLE tb_user (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  student_no  VARCHAR(20)     NOT NULL COMMENT '学号（登录标识）',
  password    VARCHAR(128)    DEFAULT NULL COMMENT '密码（加密存储，可空=仅验证码登录）',
  nick_name   VARCHAR(32)     NOT NULL COMMENT '昵称',
  icon        VARCHAR(255)    DEFAULT NULL COMMENT '头像',
  phone       VARCHAR(11)     DEFAULT NULL COMMENT '手机号（接收验证码）',
  role        TINYINT         NOT NULL DEFAULT 0 COMMENT '角色：0-学生 1-组织方 2-管理员',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_no (student_no)
) ENGINE=InnoDB AUTO_INCREMENT=1001 COMMENT='用户表';

-- -------------------------------------------------------------
-- 2. 用户详情表（P0）<- tb_user_info
-- -------------------------------------------------------------
CREATE TABLE tb_user_info (
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  total_hours DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '累计志愿时长（小时）',
  credits     INT             NOT NULL DEFAULT 0 COMMENT '志愿积分',
  level       INT             NOT NULL DEFAULT 0 COMMENT '志愿等级：0-4 星级志愿者',
  fans        INT             NOT NULL DEFAULT 0 COMMENT '粉丝数',
  follows     INT             NOT NULL DEFAULT 0 COMMENT '关注数',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB COMMENT='用户详情表（志愿者画像）';

-- -------------------------------------------------------------
-- 3. 社团/组织表（P0，全新）
-- -------------------------------------------------------------
CREATE TABLE tb_org (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '组织ID',
  name        VARCHAR(64)     NOT NULL COMMENT '组织名称',
  icon        VARCHAR(255)    DEFAULT NULL COMMENT '组织Logo',
  description VARCHAR(512)    DEFAULT NULL COMMENT '组织简介',
  owner_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '负责人用户ID',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='社团/组织表';

-- -------------------------------------------------------------
-- 4. 活动分类表（P0）<- tb_shop_type
-- -------------------------------------------------------------
CREATE TABLE tb_activity_type (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  name        VARCHAR(32)     NOT NULL COMMENT '分类名称',
  icon        VARCHAR(255)    DEFAULT NULL COMMENT '分类图标',
  sort        INT             NOT NULL DEFAULT 0 COMMENT '排序权重',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='活动分类表';

-- -------------------------------------------------------------
-- 5. 活动表（P0）<- tb_shop + tb_voucher + tb_seckill_voucher 融合
--    说明：原项目"券+秒杀券"两张表按需关联，活动平台名额/报名窗口
--    是活动的必备属性，故合并进一张表，减少联表。
-- -------------------------------------------------------------
CREATE TABLE tb_activity (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  org_id            BIGINT UNSIGNED NOT NULL COMMENT '主办方组织ID',
  type_id           BIGINT UNSIGNED NOT NULL COMMENT '活动分类ID',
  title             VARCHAR(128)    NOT NULL COMMENT '活动标题',
  images            VARCHAR(1024)   DEFAULT NULL COMMENT '活动图片，多张逗号分隔',
  content           TEXT            COMMENT '活动详情（图文）',
  address           VARCHAR(255)    DEFAULT NULL COMMENT '活动地点',
  x                 DECIMAL(10,6)   DEFAULT NULL COMMENT '经度（GEO 附近活动预留）',
  y                 DECIMAL(10,6)   DEFAULT NULL COMMENT '纬度（GEO 附近活动预留）',
  begin_time        DATETIME        NOT NULL COMMENT '活动开始时间',
  end_time          DATETIME        NOT NULL COMMENT '活动结束时间',
  enroll_start_time DATETIME        DEFAULT NULL COMMENT '报名开始时间（NULL=发布即可报名）',
  enroll_end_time   DATETIME        DEFAULT NULL COMMENT '报名截止时间',
  total_quota       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总名额（0=不限额）',
  left_quota        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '剩余名额',
  volunteer_hours   DECIMAL(4,1)    NOT NULL DEFAULT 0 COMMENT '可认定志愿时长（小时）',
  status            TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-报名中 2-进行中 3-已结束 4-已取消',
  create_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_type (type_id),
  KEY idx_org (org_id),
  KEY idx_begin_time (begin_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='活动表';

-- -------------------------------------------------------------
-- 6. 报名记录表（P0）<- tb_voucher_order
--    id 由 RedisIdWorker 生成全局唯一ID（非自增）
--    uk_user_activity 唯一索引 = "一人一活动"数据库兜底
--    （取消报名采用"事务内先删后插"策略，重新报名不冲突）
-- -------------------------------------------------------------
CREATE TABLE tb_enrollment (
  id          BIGINT UNSIGNED NOT NULL COMMENT '报名单号（RedisIdWorker 全局ID）',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  activity_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
  status      TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-已报名 1-已签到 2-已完成(时长已认定) 3-已取消',
  enroll_time DATETIME        NOT NULL COMMENT '报名时间',
  sign_time   DATETIME        DEFAULT NULL COMMENT '现场签到时间',
  finish_time DATETIME        DEFAULT NULL COMMENT '完成时间（时长认定时间）',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_activity (user_id, activity_id),
  KEY idx_activity (activity_id)
) ENGINE=InnoDB COMMENT='报名记录表';

-- -------------------------------------------------------------
-- 7. 关注表（P1）<- tb_follow（被关注对象由用户改为组织）
-- -------------------------------------------------------------
CREATE TABLE tb_follow (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '关注者',
  follow_org_id BIGINT UNSIGNED NOT NULL COMMENT '被关注组织',
  create_time   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_org (user_id, follow_org_id)
) ENGINE=InnoDB COMMENT='关注表';

-- -------------------------------------------------------------
-- 8. 活动帖表（P1）<- tb_blog
-- -------------------------------------------------------------
CREATE TABLE tb_post (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '发布者',
  activity_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联活动（NULL=普通分享）',
  title       VARCHAR(128)    DEFAULT NULL COMMENT '标题',
  content     VARCHAR(2048)   NOT NULL COMMENT '正文',
  images      VARCHAR(1024)   DEFAULT NULL COMMENT '图片，逗号分隔',
  liked       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数（冗余计数）',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_activity (activity_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='活动回顾帖/志愿心得表';

-- -------------------------------------------------------------
-- 9. 帖子评论表（P1）<- tb_blog_comments
-- -------------------------------------------------------------
CREATE TABLE tb_post_comment (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  post_id     BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '评论者',
  parent_id   BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID（顶级评论为NULL）',
  answer_id   BIGINT UNSIGNED DEFAULT NULL COMMENT '回复目标用户ID',
  content     VARCHAR(512)    NOT NULL COMMENT '评论内容',
  liked       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_post (post_id)
) ENGINE=InnoDB COMMENT='帖子评论表';

-- -------------------------------------------------------------
-- 10. 帖子点赞表（P1）<- blog_like（修正字段拼写）
-- -------------------------------------------------------------
CREATE TABLE tb_post_like (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  post_id     BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '点赞用户',
  create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB COMMENT='帖子点赞表';

-- =============================================================
-- 演示数据（便于启动后直接调试）
-- =============================================================

-- 用户：1管理员 + 2组织方 + 5学生（密码统一为空=验证码登录）
INSERT INTO tb_user (id, student_no, nick_name, phone, role) VALUES
(1, 'admin0000', '平台管理员', '13800000000', 2),
(2, 'T2021001',  '青志协·王老师', '13800000001', 1),
(3, 'T2021002',  '文体部·李老师', '13800000002', 1),
(1001, '20230001', '陈小星', '13800001001', 0),
(1002, '20230002', '林晓',   '13800001002', 0),
(1003, '20230003', '赵雨桐', '13800001003', 0),
(1004, '20230004', '孙一帆', '13800001004', 0),
(1005, '20230005', '周梦琪', '13800001005', 0);

-- 用户详情
INSERT INTO tb_user_info (user_id, total_hours, credits, level, fans, follows) VALUES
(1001, 36.5, 180, 2, 12, 3),
(1002, 52.0, 260, 3, 25, 5),
(1003, 18.0, 90,  1, 5,  2),
(1004, 8.5,  40,  0, 1,  1),
(1005, 44.0, 220, 2, 18, 4);

-- 组织
INSERT INTO tb_org (id, name, icon, description, owner_id) VALUES
(1, '青年志愿者协会', NULL, '组织校内外公益志愿活动，认定志愿时长', 2),
(2, '校学生会文体部', NULL, '组织文艺演出、体育竞赛等校园文体活动', 3),
(3, '计算机科技协会', NULL, '技术讲座、编程竞赛与科技实践', NULL);

-- 活动分类
INSERT INTO tb_activity_type (id, name, sort) VALUES
(1, '讲座沙龙', 1),
(2, '文体竞赛', 2),
(3, '公益志愿', 3),
(4, '社会实践', 4);

-- 活动（坐标为示例值，用于 GEO 功能演示）
INSERT INTO tb_activity (id, org_id, type_id, title, address, x, y,
  begin_time, end_time, enroll_start_time, enroll_end_time,
  total_quota, left_quota, volunteer_hours, status, content) VALUES
(1, 1, 3, '图书馆图书整理志愿服务', '校图书馆一层', 114.360100, 30.530100,
  '2026-10-15 14:00:00', '2026-10-15 17:00:00', '2026-10-11 08:00:00', '2026-10-14 12:00:00',
  20, 17, 3.0, 1, '协助图书馆整理新书入库与归架，认定志愿时长3小时。'),
(2, 1, 3, '社区敬老院慰问陪伴', '市第二敬老院', 114.410200, 30.560200,
  '2026-10-18 09:00:00', '2026-10-18 12:00:00', '2026-10-11 08:00:00', '2026-10-16 18:00:00',
  15, 15, 3.5, 1, '陪伴老人聊天、表演节目、打扫院区。统一乘车前往。'),
(3, 3, 1, 'AI 大模型应用开发入门讲座', '计算机学院报告厅', 114.350300, 30.525300,
  '2026-10-25 19:00:00', '2026-10-25 21:00:00', '2026-10-12 00:00:00', '2026-10-24 18:00:00',
  200, 198, 0, 1, '面向全校的 AI 技术科普讲座，不认定志愿时长。'),
(4, 2, 2, '秋季校园马拉松志愿服务', '校田径场', 114.355400, 30.528400,
  '2026-10-11 07:30:00', '2026-10-11 12:00:00', '2026-10-01 00:00:00', '2026-10-09 18:00:00',
  50, 0, 4.5, 2, '补给站服务、路线引导、医疗协助。'),
(5, 1, 3, '迎新季引导志愿者', '校东门迎新点', 114.365500, 30.535500,
  '2026-09-20 08:00:00', '2026-09-20 17:00:00', '2026-09-15 00:00:00', '2026-09-19 12:00:00',
  30, 0, 8.0, 3, '已结束：新生报到引导与行李搬运协助。');

-- 报名记录（id 模拟 RedisIdWorker 全局ID）
INSERT INTO tb_enrollment (id, user_id, activity_id, status, enroll_time, sign_time, finish_time) VALUES
(20261011000001, 1001, 1, 0, '2026-10-11 09:00:00', NULL, NULL),
(20261011000002, 1002, 1, 0, '2026-10-11 09:05:00', NULL, NULL),
(20261011000003, 1003, 1, 0, '2026-10-11 09:10:00', NULL, NULL),
(20261001000001, 1001, 4, 2, '2026-10-01 10:00:00', '2026-10-11 07:20:00', '2026-10-11 12:05:00'),
(20261001000002, 1005, 4, 2, '2026-10-01 11:00:00', '2026-10-11 07:25:00', '2026-10-11 12:05:00'),
(20260915000001, 1002, 5, 2, '2026-09-15 08:00:00', '2026-09-20 07:50:00', '2026-09-20 17:10:00');

-- 关注
INSERT INTO tb_follow (user_id, follow_org_id) VALUES
(1001, 1), (1001, 3), (1002, 1), (1005, 2);

-- 帖子
INSERT INTO tb_post (id, user_id, activity_id, title, content, liked) VALUES
(1, 1001, 5, '迎新志愿一天记', '今天引导了 30 多位新生报到，虽然累但超有成就感，8 小时志愿时长到账！', 12),
(2, 1002, NULL, '志愿时长认定流程答疑', '统一回复：活动结束签到后 48 小时内自动计入累计时长，无需手动申请。', 5);

-- 评论
INSERT INTO tb_post_comment (id, post_id, user_id, content) VALUES
(1, 1, 1003, '学长辛苦了！下次带我一个');

-- 点赞
INSERT INTO tb_post_like (post_id, user_id) VALUES
(1, 1003), (1, 1004), (1, 1005), (2, 1001);
