-- picture-backend 用户 VIP 到期时间（迁移）

USE mydb;

ALTER TABLE `user`
    ADD COLUMN `vipExpireTime` DATETIME DEFAULT NULL COMMENT 'VIP 到期时间，NULL=从未开通或已过期清除前仍可保留历史' AFTER `userRole`;
