-- picture-backend 图片表

USE mydb;

DROP TABLE IF EXISTS `picture`;

CREATE TABLE `picture` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `url`         VARCHAR(512) NOT NULL COMMENT '图片访问地址',
    `name`        VARCHAR(256) NOT NULL COMMENT '原始文件名',
    `size`        BIGINT       NOT NULL COMMENT '文件大小(字节)',
    `width`       INT                   DEFAULT NULL COMMENT '图片宽度(px)',
    `height`      INT                   DEFAULT NULL COMMENT '图片高度(px)',
    `contentType` VARCHAR(64)  NOT NULL COMMENT 'MIME 类型',
    `format`      VARCHAR(32)           DEFAULT NULL COMMENT '图片格式后缀',
    `userId`      BIGINT       NOT NULL COMMENT '上传人ID',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='图片表';
