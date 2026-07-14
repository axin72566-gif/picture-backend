-- picture-backend 聊天会话表

USE mydb;

CREATE TABLE IF NOT EXISTS `conversation` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`       VARCHAR(16) NOT NULL COMMENT 'SPACE / DM',
    `spaceId`    BIGINT               DEFAULT NULL COMMENT 'SPACE 时关联空间，DM 为空',
    `createTime` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spaceId` (`spaceId`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聊天会话表';
