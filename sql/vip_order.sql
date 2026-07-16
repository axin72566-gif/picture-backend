-- picture-backend VIP 订单表

USE mydb;

CREATE TABLE IF NOT EXISTS `vip_order` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `orderNo`      VARCHAR(64)  NOT NULL COMMENT '订单号',
    `userId`       BIGINT       NOT NULL COMMENT '下单用户ID',
    `planId`       BIGINT       NOT NULL COMMENT '套餐ID',
    `durationDays`         INT          NOT NULL COMMENT '下单时套餐天数快照',
    `originalAmountCents`  INT          NOT NULL COMMENT '套餐原价快照（分）',
    `discountCents`        INT          NOT NULL DEFAULT 0 COMMENT '优惠减免（分）',
    `couponId`             BIGINT                DEFAULT NULL COMMENT '使用的用户券ID',
    `amountCents`          INT          NOT NULL COMMENT '实付金额快照（分）',
    `status`               VARCHAR(16)  NOT NULL COMMENT 'PENDING / PAID / CANCELLED / EXPIRED',
    `expireTime`   DATETIME     NOT NULL COMMENT '支付截止时间（创建+15分钟）',
    `paidTime`     DATETIME              DEFAULT NULL COMMENT '支付成功时间',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orderNo` (`orderNo`),
    KEY `idx_userId` (`userId`),
    KEY `idx_user_status` (`userId`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='VIP 订单表';
