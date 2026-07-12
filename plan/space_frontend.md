# 前端团队空间适配计划

## 选定方案

- **项目**：`picture-frontend`（Vue 3 + Vite + Pinia + Naive UI + vue-router）
- **范围**：对齐后端成员体系（创建空间、邀请同意后加入、角色 CREATOR/EDITOR/VIEWER）；不绑定图片、不做空间内图库
- **风格**：复用关注列表 / 通知页 / AppHeader 的布局与交互

## 路由

| 路由 | 鉴权 | 页面 |
|------|------|------|
| `/spaces` | 需登录 | 我的空间列表 + 创建 |
| `/spaces/invites` | 需登录 | 我收到的待处理邀请 |
| `/spaces/:id` | 需登录 | 空间详情（成员 / 邀请管理） |

`/spaces/invites` 注册在 `/spaces/:id` 之前。

## 代码结构

- `src/types/space.ts`：角色、邀请状态、VO / DTO
- `src/api/space.ts`：封装全部 `/api/space/**`
- `src/utils/space.ts`：角色中文标签
- `src/pages/space/SpaceListPage.vue`
- `src/pages/space/SpaceInvitePendingPage.vue`
- `src/pages/space/SpaceDetailPage.vue`
- `src/components/AppHeader.vue`：导航「空间」+ 下拉「我的空间」
- 通知：`SPACE_INVITE` 类型 / 文案 / 点击跳转 `/spaces/invites`

## 交互摘要

1. 创建空间 → 进入详情
2. 创建者邀请（账号或用户 ID）→ 被邀请人在待处理页同意/拒绝
3. 创建者可改角色（EDITOR/VIEWER）、踢人、取消邀请、编辑/解散
4. 非创建者可退出；`SPACE_INVITE` 通知深链到待处理邀请页
