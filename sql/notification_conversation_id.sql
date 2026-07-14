-- picture-backend 通知表增加 conversationId（聊天 @ 深链，P3）

USE mydb;

ALTER TABLE `notification`
    ADD COLUMN `conversationId` BIGINT DEFAULT NULL COMMENT '关联会话ID，非聊天通知为 NULL' AFTER `spaceId`;
