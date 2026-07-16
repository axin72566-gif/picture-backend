-- picture-backend 用户表
-- 数据库: mydb (与 docker-compose / application.yml 一致)

CREATE DATABASE IF NOT EXISTS mydb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mydb;

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `userAccount` VARCHAR(16)  NOT NULL COMMENT '账号',
    `userPassword` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `userName`    VARCHAR(32)           DEFAULT NULL COMMENT '昵称',
    `userAvatar`  VARCHAR(255)          DEFAULT NULL COMMENT '头像 URL',
    `userProfile` VARCHAR(255)          DEFAULT NULL COMMENT '个人简介',
    `userRole`    VARCHAR(16)  NOT NULL DEFAULT 'user' COMMENT '角色: user / admin',
    `vipExpireTime` DATETIME            DEFAULT NULL COMMENT 'VIP 到期时间，NULL=从未开通',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_userAccount` (`userAccount`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';