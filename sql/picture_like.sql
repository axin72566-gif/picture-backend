CREATE TABLE `picture_like` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `userId`     BIGINT   NOT NULL COMMENT '点赞用户ID',
    `pictureId`  BIGINT   NOT NULL COMMENT '被赞图片ID',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDelete`   TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_picture` (`userId`, `pictureId`),
    KEY `idx_pictureId` (`pictureId`),
    KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
