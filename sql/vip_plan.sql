-- picture-backend VIP 套餐表

USE mydb;

CREATE TABLE IF NOT EXISTS `vip_plan` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`         VARCHAR(32)  NOT NULL COMMENT '套餐编码',
    `name`         VARCHAR(64)  NOT NULL COMMENT '套餐名称',
    `durationDays` INT          NOT NULL COMMENT '开通/续期天数',
    `priceCents`   INT          NOT NULL COMMENT '价格（分）',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='VIP 套餐表';

INSERT INTO `vip_plan` (`code`, `name`, `durationDays`, `priceCents`, `status`)
VALUES ('MONTH', '月度 VIP', 30, 1800, 1),
       ('QUARTER', '季度 VIP', 90, 4800, 1),
       ('YEAR', '年度 VIP', 365, 16800, 1)
ON DUPLICATE KEY UPDATE `name`         = VALUES(`name`),
                        `durationDays` = VALUES(`durationDays`),
                        `priceCents`   = VALUES(`priceCents`),
                        `status`       = VALUES(`status`);
