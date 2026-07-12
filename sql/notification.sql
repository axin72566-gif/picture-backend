-- picture-backend 站内通知表

USE mydb;

CREATE TABLE IF NOT EXISTS `notification` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `receiverId` BIGINT       NOT NULL COMMENT '接收者用户ID',
    `senderId`   BIGINT       NOT NULL COMMENT '触发者用户ID',
    `type`       VARCHAR(32)  NOT NULL COMMENT '类型：FOLLOW / COMMENT / REPLY / LIKE / SPACE_INVITE',
    `pictureId`  BIGINT                DEFAULT NULL COMMENT '关联图片ID，关注通知为 NULL',
    `commentId`  BIGINT                DEFAULT NULL COMMENT '关联评论ID，关注通知为 NULL',
    `spaceId`    BIGINT                DEFAULT NULL COMMENT '关联空间ID，非空间通知为 NULL',
    `content`    VARCHAR(100)          DEFAULT NULL COMMENT '内容摘要，关注通知可空',
    `isRead`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `isDelete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_read_time` (`receiverId`, `isRead`, `createTime`),
    KEY `idx_receiver_time` (`receiverId`, `createTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='站内通知表';
