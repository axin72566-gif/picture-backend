# 企业 IM 演进 — 第二期（会话闭环）

## 0. 大白话

P1 打好地基（会话壳、空间群、未读、全局 WS）。P2 补上「点人发消息」：

- 任意登录用户可与另一用户 **get-or-create** 私聊（同一对用户只有一个 DM）
- 「消息」列表同时展示空间群与私聊
- 用户资料页、空间成员列表可发起私聊

仍不做：@、已读回执、多媒体、搜索、置顶/免打扰、删除会话。

---

## 1. 模型

| 规则 | 约定 |
|------|------|
| 谁可聊谁 | 任意已登录用户 ↔ 其他用户（不能自己聊自己） |
| 唯一性 | `conversation_dm_pair(userLowId, userHighId)` → `conversationId` |
| 成员 | 恰好 2 人 |
| 删消息 | 仅本人（SPACE 仍允许 CREATOR 删他人） |
| 退出私聊 | P2 不做 |

SQL：`sql/conversation_dm_pair.sql`。

---

## 2. API

`POST /api/chat/conversations/dm` body `{ "peerUserId": 42 }`

- 登录；peer 存在且 ≠ 自己
- 有 pair → 返回已有 `ConversationVO`
- 无 → 同事务创建 `conversation(type=DM)` + 两行 member + dm_pair
- 首次创建：双方推 `CONVERSATION_UPDATED`

`GET /api/chat/conversations` / 会话 VO 增强：

| 字段 | SPACE | DM |
|------|-------|-----|
| `spaceId` / `spaceName` | 有 | null |
| `peer` | null | 对方 UserVO |
| `title` | 空间名 | 对方昵称 |

---

## 3. 前端

- `chatStore.openDm(peerUserId)` → `/messages/:id`
- 资料页「发消息」、空间成员「私聊」
- 列表：DM 头像+昵称；无消息时「已创建会话」
- 房间：DM 标题为对方昵称；「查看资料」→ `/user/{peerId}`

---

## 4. 验收

- [ ] 资料页发消息 → 进入私聊并可互发
- [ ] 再点一次 → 同一 `conversationId`
- [ ] 列表同时有群与私聊，未读正确
- [ ] 切 Tab 仍能收私聊推送
- [ ] 不能对自己发起；对方不存在报错
- [ ] DM 只能删自己的；空间群 CREATOR 删他人仍可用

详见实现计划与代码：`com.example.picturebackend.chat`、前端 `stores/chatStore.ts`。
