-- picture-backend 会话成员表（含已读水位）

USE mydb;

CREATE TABLE IF NOT EXISTS `conversation_member` (
    `id`                 BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversationId`     BIGINT   NOT NULL COMMENT '会话ID',
    `userId`             BIGINT   NOT NULL COMMENT '用户ID',
    `lastReadMessageId`  BIGINT   NOT NULL DEFAULT 0 COMMENT '已读水位消息ID，0表示未读过',
    `joinedAt`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `isDelete`           TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_user` (`conversationId`, `userId`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='会话成员表';
