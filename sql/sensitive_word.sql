-- picture-backend 敏感词表（IM P4）

USE mydb;

CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `word`       VARCHAR(64)  NOT NULL COMMENT '敏感词',
    `enabled`    TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聊天敏感词表';
