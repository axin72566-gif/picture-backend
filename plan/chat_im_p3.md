# 企业 IM 演进 — 第三期（发图 + @）

## 0. 大白话

P1+P2：群/私聊、未读、全局 WS、纯文本+回复。

P3 补上：
1. 聊天发图（jpeg/png/gif/webp，COS，不进图库）
2. @ 成员：候选列表、气泡高亮、站内通知 `CHAT_MENTION` 深链进会话

不做：已读回执、搜索、发文件、置顶/免打扰、@所有人。

---

## 1. 模型

- `chat_message`：`messageType` TEXT/IMAGE + `media*`（`sql/chat_message_p3.sql`）
- `chat_message_mention`（`sql/chat_message_mention.sql`）
- `notification.conversationId` + 类型 `CHAT_MENTION`（`sql/notification_conversation_id.sql`）

COS key：`chat/{yyyy}/{mm}/{dd}/{uuid}{ext}`。删消息仅软删，不强制删 COS。

---

## 2. API

| 接口 | 说明 |
|------|------|
| `GET /api/chat/conversations/{id}/members` | @ 选人 |
| `POST .../messages` | 文本；body 可含 `mentionUserIds` |
| `POST .../messages/image` | multipart：`file` + 可选 `caption`/`replyToId`/`clientMsgId`/`mentionUserIds` |

列表预览：IMAGE 无配文 → `[图片]`。

---

## 3. 前端

- `SpaceChatSection`：图片按钮、@ 浮层、图片气泡、提及高亮
- `ChatListPage`：图片预览文案
- 通知：`CHAT_MENTION` → `/messages/{conversationId}`

---

## 4. 验收

- [ ] 群/私聊发图实时可见；列表为 `[图片]` 或配文
- [ ] 同 `clientMsgId` 不双条
- [ ] @ 成员有通知并可进会话；不能 @ 非成员/自己
- [ ] 非图片或超 10MB 被拒
