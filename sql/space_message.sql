-- picture-backend 团队空间群聊消息表

USE mydb;

CREATE TABLE IF NOT EXISTS `space_message` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spaceId`    BIGINT       NOT NULL COMMENT '空间ID',
    `userId`     BIGINT       NOT NULL COMMENT '发送者用户ID',
    `content`    VARCHAR(500) NOT NULL COMMENT '消息内容',
    `replyToId`  BIGINT                DEFAULT NULL COMMENT '回复的消息ID，可空',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_space_createTime` (`spaceId`, `createTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='团队空间群聊消息表';
