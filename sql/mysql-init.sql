-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `username`     VARCHAR(50)  NOT NULL,
    `password`     VARCHAR(255) NOT NULL  COMMENT 'BCrypt 加密',
    `email`        VARCHAR(100) DEFAULT NULL,
    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 知识库表（每个用户可以创建多个知识库，这是多租户隔离的核心）
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL  COMMENT '所属用户，租户隔离靠这个字段',
    `name`         VARCHAR(100) NOT NULL,
    `description`  TEXT         DEFAULT NULL,
    `doc_count`    INT          NOT NULL DEFAULT 0 COMMENT '文档数量',
    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS `document` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `knowledge_base_id` BIGINT      NOT NULL,
    `user_id`          BIGINT       NOT NULL  COMMENT '冗余字段，方便查询',
    `file_name`        VARCHAR(255) NOT NULL,
    `file_type`        VARCHAR(20)  NOT NULL  COMMENT 'txt / md / pdf',
    `file_size`        BIGINT       NOT NULL  COMMENT '字节',
    `file_path`        VARCHAR(500) NOT NULL  COMMENT '本地存储路径',
    `status`           TINYINT      NOT NULL DEFAULT 0
    COMMENT '0=待处理 1=向量化中 2=完成 3=失败',
    `chunk_count`      INT          DEFAULT 0 COMMENT '分块数量',
    `error_msg`        TEXT         DEFAULT NULL COMMENT '失败原因',
    `created_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`knowledge_base_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS `conversation` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT       NOT NULL,
    `knowledge_base_id` BIGINT      NOT NULL,
    `title`            VARCHAR(200) DEFAULT NULL COMMENT '自动取第一个问题作为标题',
    `created_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_kb_id` (`knowledge_base_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT       NOT NULL,
    `user_id`         BIGINT       NOT NULL,
    `role`            VARCHAR(20)  NOT NULL  COMMENT 'user / assistant',
    `content`         TEXT         NOT NULL,
    `tokens_used`     INT          DEFAULT 0 COMMENT '消耗 token 数（第三阶段计费用）',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_user_id_created` (`user_id`, `created_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';