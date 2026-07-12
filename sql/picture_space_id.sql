-- picture 表增加 spaceId，供团队空间图片归属；NULL 表示个人图

USE mydb;

ALTER TABLE `picture`
    ADD COLUMN `spaceId` BIGINT DEFAULT NULL COMMENT '所属空间ID，个人图为 NULL' AFTER `userId`,
    ADD KEY `idx_spaceId` (`spaceId`);
