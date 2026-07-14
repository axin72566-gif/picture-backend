-- picture-backend 私聊用户对唯一映射表（P2）

USE mydb;

CREATE TABLE IF NOT EXISTS `conversation_dm_pair` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `userLowId`      BIGINT   NOT NULL COMMENT '较小用户 ID',
    `userHighId`     BIGINT   NOT NULL COMMENT '较大用户 ID',
    `conversationId` BIGINT   NOT NULL COMMENT '对应 DM 会话 ID',
    `createTime`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_pair` (`userLowId`, `userHighId`),
    UNIQUE KEY `uk_conversationId` (`conversationId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='私聊用户对唯一映射';
