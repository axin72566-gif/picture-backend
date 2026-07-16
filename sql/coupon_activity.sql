-- picture-backend 优惠券秒杀活动表

USE mydb;

CREATE TABLE IF NOT EXISTS `coupon_activity` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`             VARCHAR(64)  NOT NULL COMMENT '活动名称',
    `discountCents`    INT          NOT NULL COMMENT '固定减免金额（分）',
    `totalStock`       INT          NOT NULL COMMENT '总库存',
    `claimedCount`     INT          NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `startTime`        DATETIME     NOT NULL COMMENT '开始时间',
    `endTime`          DATETIME     NOT NULL COMMENT '结束时间',
    `couponValidDays`  INT          NOT NULL COMMENT '领取后有效天数',
    `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '1上架 0下架',
    `createTime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `startTime`, `endTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='优惠券秒杀活动表';

-- 演示活动：减 5 元，库存 100，领取后 7 天有效（按需改时间后上架）
INSERT INTO `coupon_activity` (`name`, `discountCents`, `totalStock`, `claimedCount`,
                               `startTime`, `endTime`, `couponValidDays`, `status`)
SELECT 'VIP 满减秒杀', 500, 100, 0,
       NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 7, 1
WHERE NOT EXISTS (
    SELECT 1 FROM `coupon_activity` WHERE `name` = 'VIP 满减秒杀' AND `isDelete` = 0
);
