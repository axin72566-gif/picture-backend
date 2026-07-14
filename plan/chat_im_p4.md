# 企业 IM 演进 — 第四期（治理收官）

## 0. 说明

本项目企业 IM 路线图最后一期：敏感词拦截 + 拦截审计。不做机器人、已读回执、搜索。

| 期次 | 主题 | 状态 |
|------|------|------|
| P1 | 地基 | 完成 |
| P2 | 私聊 | 完成 |
| P3 | 发图 + @ | 完成 |
| P4 | 敏感词 + 审计 | **本期（完结）** |

---

## 1. 能力

- 发文本 / 图片配文命中敏感词 → 拒绝（`消息包含敏感内容`），不落库、不推送
- 每次拦截写入 `chat_moderation_log`（独立事务，外层回滚不影响审计）
- 管理员维护词库（Redis `SET chat:sensitive:words` 缓存）、查看审计
- 前端 `/admin` 两 Tab；Header 对 admin 显示「聊天治理」

SQL：`sql/sensitive_word.sql`、`sql/chat_moderation_log.sql`。

管理员：将用户 `userRole` 改为 `admin` 后重新登录。

---

## 2. API

| 方法 | 路径 | 权限 |
|------|------|------|
| GET/POST | `/api/admin/sensitive-words` | admin |
| PUT/DELETE | `/api/admin/sensitive-words/{id}` | admin |
| GET | `/api/admin/chat/moderation-logs` | admin |

---

## 3. 验收

- [ ] 含敏感词发送失败，聊天无新消息
- [ ] 审计有 BLOCK 记录
- [ ] 增删词后对新发送立即生效
- [ ] 非 admin 调管理接口无权限
- [ ] 空配文发图不被误拦
