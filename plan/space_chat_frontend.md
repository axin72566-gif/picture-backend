# 前端空间群聊适配

## 选定方案

- **项目**：`picture-frontend`（Vue 3 + Vite + Pinia + Naive UI）
- **入口**：空间详情页第 4 个 Tab「群聊」（不新增路由）
- **实时**：`@stomp/stompjs`；开发环境经 Vite 代理 `/ws`，生产由 `VITE_API_BASE_URL` 派生 `ws(s)://…/ws?token=`
- **写路径**：发/删走 REST；本端用响应乐观更新，WS 推送按 `id` 去重

## 能力

| 能力 | 说明 |
|------|------|
| 历史 | `GET /api/space/{id}/messages`，desc 翻转正序；「加载更早」prepend |
| 发送 / 回复 | `POST`，`content` + 可选 `replyToId`；最长 500 |
| 删除 | 本人或 CREATOR；`DELETE /api/space/{id}/messages/{messageId}` |
| 实时 | 订阅 `/topic/space.{id}`，处理 `MESSAGE_NEW` / `MESSAGE_DELETED` |

## 主要文件

- `src/types/spaceMessage.ts`
- `src/api/space.ts`（messages 三个接口）
- `src/utils/space.ts`（`canDeleteSpaceMessage`）
- `src/utils/spaceChat.ts`（`buildSpaceChatWsUrl`）
- `src/composables/useSpaceChat.ts`
- `src/components/SpaceChatSection.vue`
- `src/pages/space/SpaceDetailPage.vue`（群聊 Tab）
- `vite.config.ts`（`/ws` 代理）
