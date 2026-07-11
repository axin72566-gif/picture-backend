-- picture-backend 图片评论表（楼中楼）

USE mydb;

CREATE TABLE IF NOT EXISTS `picture_comment` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pictureId`  BIGINT       NOT NULL COMMENT '图片ID',
    `userId`     BIGINT       NOT NULL COMMENT '评论用户ID',
    `content`    VARCHAR(500) NOT NULL COMMENT '评论内容',
    `parentId`   BIGINT                DEFAULT NULL COMMENT '被回复评论ID，根评论为 NULL',
    `rootId`     BIGINT                DEFAULT NULL COMMENT '所属根评论ID，根评论为 NULL',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_pictureId_root` (`pictureId`, `rootId`, `createTime`),
    KEY `idx_rootId` (`rootId`, `createTime`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='图片评论表';
