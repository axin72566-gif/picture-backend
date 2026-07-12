-- picture-backend 团队空间成员表

USE mydb;

CREATE TABLE IF NOT EXISTS `space_member` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spaceId`    BIGINT      NOT NULL COMMENT '空间ID',
    `userId`     BIGINT      NOT NULL COMMENT '成员用户ID',
    `role`       VARCHAR(32) NOT NULL COMMENT '角色：CREATOR / EDITOR / VIEWER',
    `createTime` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `isDelete`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_space_user` (`spaceId`, `userId`),
    KEY `idx_userId` (`userId`),
    KEY `idx_spaceId` (`spaceId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='团队空间成员表';
