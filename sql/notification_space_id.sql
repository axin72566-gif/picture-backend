-- notification 表增加 spaceId，供空间邀请通知深链

USE mydb;

ALTER TABLE `notification`
    ADD COLUMN `spaceId` BIGINT DEFAULT NULL COMMENT '关联空间ID，非空间通知为 NULL' AFTER `commentId`;
