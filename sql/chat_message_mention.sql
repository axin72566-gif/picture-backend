-- picture-backend 聊天消息 @ 提及表（P3）

USE mydb;

CREATE TABLE IF NOT EXISTS `chat_message_mention` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `messageId`  BIGINT   NOT NULL COMMENT '消息 ID',
    `userId`     BIGINT   NOT NULL COMMENT '被 @ 用户 ID',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`messageId`, `userId`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聊天消息提及表';
