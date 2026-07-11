CREATE TABLE `user_follow` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `followerId`  BIGINT   NOT NULL COMMENT '关注者ID',
    `followedId`  BIGINT   NOT NULL COMMENT '被关注者ID',
    `createTime`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDelete`    TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_followed` (`followerId`, `followedId`),
    KEY `idx_followerId` (`followerId`),
    KEY `idx_followedId` (`followedId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
