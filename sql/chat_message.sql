-- picture-backend 聊天消息表

USE mydb;

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversationId` BIGINT       NOT NULL COMMENT '会话ID',
    `senderId`       BIGINT       NOT NULL COMMENT '发送者用户ID',
    `content`        VARCHAR(500) NOT NULL COMMENT '消息内容',
    `replyToId`      BIGINT                DEFAULT NULL COMMENT '回复的消息ID，可空',
    `clientMsgId`    VARCHAR(64)           DEFAULT NULL COMMENT '客户端幂等ID，可空',
    `createTime`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sender_clientMsg` (`senderId`, `clientMsgId`),
    KEY `idx_conversation_id` (`conversationId`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聊天消息表';
