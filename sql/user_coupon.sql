-- picture-backend 用户优惠券表

USE mydb;

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activityId`     BIGINT       NOT NULL COMMENT '活动ID',
    `userId`         BIGINT       NOT NULL COMMENT '用户ID',
    `discountCents`  INT          NOT NULL COMMENT '减免金额快照（分）',
    `status`         VARCHAR(16)  NOT NULL COMMENT 'UNUSED / LOCKED / USED / EXPIRED',
    `expireTime`     DATETIME     NOT NULL COMMENT '券过期时间',
    `lockOrderNo`    VARCHAR(64)           DEFAULT NULL COMMENT '锁定/使用的 VIP 订单号',
    `createTime`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_user` (`activityId`, `userId`),
    KEY `idx_user_status` (`userId`, `status`),
    KEY `idx_lockOrderNo` (`lockOrderNo`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户优惠券表';
