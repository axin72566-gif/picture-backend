-- picture-backend 聊天拦截审计表（IM P4）

USE mydb;

CREATE TABLE IF NOT EXISTS `chat_moderation_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversationId`  BIGINT       NOT NULL COMMENT '会话ID',
    `senderId`        BIGINT       NOT NULL COMMENT '发送者用户ID',
    `messageType`     VARCHAR(16)  NOT NULL COMMENT 'TEXT / IMAGE',
    `originalContent` VARCHAR(500)          DEFAULT NULL COMMENT '被拦截原文摘要',
    `hitWords`        VARCHAR(500) NOT NULL COMMENT '命中词，逗号分隔',
    `action`          VARCHAR(16)  NOT NULL DEFAULT 'BLOCK' COMMENT '处置动作',
    `createTime`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_createTime` (`createTime`),
    KEY `idx_conversationId` (`conversationId`),
    KEY `idx_senderId` (`senderId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聊天敏感词拦截审计';
