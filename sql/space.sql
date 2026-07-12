-- picture-backend 团队空间表

USE mydb;

CREATE TABLE IF NOT EXISTS `space` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL COMMENT '空间名称',
    `description` VARCHAR(512)          DEFAULT NULL COMMENT '空间简介',
    `ownerId`     BIGINT       NOT NULL COMMENT '创建者用户ID',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_ownerId` (`ownerId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='团队空间表';
