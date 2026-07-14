-- picture-backend 聊天消息 P3 扩展（发图）

USE mydb;

ALTER TABLE `chat_message`
    ADD COLUMN `messageType`      VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT / IMAGE' AFTER `senderId`,
    ADD COLUMN `mediaUrl`         VARCHAR(512)          DEFAULT NULL COMMENT '图片 URL，TEXT 为空' AFTER `content`,
    ADD COLUMN `mediaWidth`       INT                   DEFAULT NULL COMMENT '图片宽' AFTER `mediaUrl`,
    ADD COLUMN `mediaHeight`      INT                   DEFAULT NULL COMMENT '图片高' AFTER `mediaWidth`,
    ADD COLUMN `mediaSize`        BIGINT                DEFAULT NULL COMMENT '图片字节数' AFTER `mediaHeight`,
    ADD COLUMN `mediaContentType` VARCHAR(64)           DEFAULT NULL COMMENT '图片 MIME' AFTER `mediaSize`;
