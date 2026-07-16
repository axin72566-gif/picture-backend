-- picture-backend VIP 订单增加优惠券字段（已有库迁移）

USE mydb;

ALTER TABLE `vip_order`
    ADD COLUMN `originalAmountCents` INT NOT NULL DEFAULT 0 COMMENT '套餐原价快照（分）' AFTER `durationDays`,
    ADD COLUMN `discountCents`       INT NOT NULL DEFAULT 0 COMMENT '优惠减免（分）' AFTER `originalAmountCents`,
    ADD COLUMN `couponId`            BIGINT DEFAULT NULL COMMENT '使用的用户券ID' AFTER `discountCents`;

-- 历史订单：原价视为实付金额
UPDATE `vip_order`
SET `originalAmountCents` = `amountCents`
WHERE `originalAmountCents` = 0 AND `amountCents` > 0;
