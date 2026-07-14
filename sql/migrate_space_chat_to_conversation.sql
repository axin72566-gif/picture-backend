-- 将已有 space / space_member / space_message 迁移到会话模型
-- 请先执行 conversation.sql、conversation_member.sql、chat_message.sql

USE mydb;

-- 1. 为每个未删空间创建 SPACE 会话（已存在则跳过）
INSERT INTO `conversation` (`type`, `spaceId`, `createTime`, `updateTime`, `isDelete`)
SELECT 'SPACE', s.`id`, s.`createTime`, s.`updateTime`, 0
FROM `space` s
WHERE s.`isDelete` = 0
  AND NOT EXISTS (
      SELECT 1 FROM `conversation` c WHERE c.`spaceId` = s.`id` AND c.`isDelete` = 0
  );

-- 2. 同步空间成员到会话成员
INSERT INTO `conversation_member` (`conversationId`, `userId`, `lastReadMessageId`, `joinedAt`, `isDelete`)
SELECT c.`id`, sm.`userId`, 0, sm.`createTime`, 0
FROM `space_member` sm
         INNER JOIN `conversation` c ON c.`spaceId` = sm.`spaceId` AND c.`type` = 'SPACE' AND c.`isDelete` = 0
WHERE sm.`isDelete` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `conversation_member` cm
      WHERE cm.`conversationId` = c.`id`
        AND cm.`userId` = sm.`userId`
  );

-- 3. 迁移历史消息（按原 id 保留，便于对照；若 chat_message 已有数据请勿重复执行）
INSERT INTO `chat_message` (`id`, `conversationId`, `senderId`, `content`, `replyToId`, `clientMsgId`,
                            `createTime`, `updateTime`, `isDelete`)
SELECT sm.`id`,
       c.`id`,
       sm.`userId`,
       sm.`content`,
       sm.`replyToId`,
       NULL,
       sm.`createTime`,
       sm.`updateTime`,
       sm.`isDelete`
FROM `space_message` sm
         INNER JOIN `conversation` c ON c.`spaceId` = sm.`spaceId` AND c.`type` = 'SPACE' AND c.`isDelete` = 0
WHERE NOT EXISTS (
    SELECT 1 FROM `chat_message` cm WHERE cm.`id` = sm.`id`
);

-- 对齐自增（若插入了显式 id）
SELECT @maxId := IFNULL(MAX(`id`), 0) FROM `chat_message`;
SET @sql = CONCAT('ALTER TABLE `chat_message` AUTO_INCREMENT = ', @maxId + 1);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
