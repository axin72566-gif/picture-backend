-- picture-backend 团队空间邀请表

USE mydb;

CREATE TABLE IF NOT EXISTS `space_invite` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spaceId`    BIGINT      NOT NULL COMMENT '空间ID',
    `inviterId`  BIGINT      NOT NULL COMMENT '邀请人用户ID',
    `inviteeId`  BIGINT      NOT NULL COMMENT '被邀请人用户ID',
    `role`       VARCHAR(32) NOT NULL COMMENT '同意后角色：EDITOR / VIEWER',
    `status`     VARCHAR(32) NOT NULL COMMENT 'PENDING / ACCEPTED / REJECTED / CANCELLED',
    `createTime` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_space_status` (`spaceId`, `status`),
    KEY `idx_invitee_status` (`inviteeId`, `status`),
    KEY `idx_inviterId` (`inviterId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='团队空间邀请表';
