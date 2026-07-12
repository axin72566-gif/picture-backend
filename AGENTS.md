# AGENTS.md

## Project
Spring Boot 3.5.3 / Java 21 / Maven, single-module scaffold for a "picture-backend" service.
- Package root: `com.example.picturebackend` (entrypoint `PictureBackendApplication`)
- Lombok is on the classpath; install/enable the Lombok plugin in the IDE or compilation will fail on getters/setters.
- `HELP.md` is generated Spring Boot boilerplate and is gitignored — safe to ignore.

## Commands
Use the Maven wrapper, not a system `mvn`:
- Build: `./mvnw clean package`
- Run app: `./mvnw spring-boot:run`
- Tests: `./mvnw test` (see "Test prerequisites" below)
- On Windows use `./mvnw.cmd` if `mvnw` isn't executable.

## Infrastructure
`compose.yaml` provisions **only MySQL** (`my-dev-mysql`, port 3306, db `mydb`, root password `123456`) and **Redis** (`my-dev-redis`, port 6379). Credentials in `src/main/resources/application.yml` match this.

The pom also pulls in `spring-boot-starter-amqp`, but **no RabbitMQ service is defined in `compose.yaml`** and there is no connection config in `application.yml`. Default autoconfig targets `localhost:5672` (RabbitMQ); startup will hang/fail unless RabbitMQ is running locally or the autoconfig is excluded.

Start MySQL before running the app or tests:
```
docker compose up -d mysql
```

## Test prerequisites
The only test is `PictureBackendApplicationTests.contextLoads`, a `@SpringBootTest`. It boots the full context, so it requires MySQL reachable at `localhost:3306` (matching `application.yml`). Without MySQL up, `./mvnw test` will fail. Redis/RabbitMQ autoconfig may also need to be excluded (e.g. via test properties) until those services exist locally.

## Conventions
- Configuration lives in `src/main/resources/application.yml`; no extra profiles or `application-*.yml` files exist yet.
- No controllers, services, entities, or repositories exist yet — this is a fresh scaffold. New code goes under `com.example.picturebackend.*`.

## User Follow
`user_follow` table stores follow relationships (camelCase columns: `id`, `followerId`, `followedId`, `createTime`, `isDelete`). See `sql/user_follow.sql`.  
取消关注使用**物理删除**（避免软删除占用 `uk_follower_followed` 唯一索引导致无法再次关注）。  
取消关注使用**物理删除**（避免软删除占用 `uk_follower_followed` 唯一索引导致无法再次关注）。

Public endpoints (no auth required): `GET /api/user/{id}/followers`, `GET /api/user/{id}/following`, `GET /api/user/{id}/follow/status`.  
`follow/status` still runs `OptionalAuthInterceptor`：有合法 token 时识别当前用户并返回是否已关注，未登录返回 `false`。  
Auth-required: `POST /api/user/follow/{followedId}`, `DELETE /api/user/follow/{followedId}`.

## Notification
`notification` table stores in-app notifications (camelCase columns: `id`, `receiverId`, `senderId`, `type`, `pictureId`, `commentId`, `spaceId`, `content`, `isRead`, `createTime`, `isDelete`). See `sql/notification.sql`.

Types: `FOLLOW`（有人关注我）、`COMMENT`（有人评论我的图片）、`REPLY`（有人回复我的评论）、`LIKE`（有人点赞我的图片）、`SPACE_INVITE`（有人邀请我加入空间）。  
写入规则：不通知自己；回复时若图片作者与父评论作者为同一人，只写一条且优先 `REPLY`。  
关注成功、发表评论、点赞成功、发起空间邀请后与业务同事务写入；取消关注 / 删除评论 / 取消点赞 / 取消或拒绝邀请不删除历史通知。

`notification` 表含可空 `spaceId`（空间邀请深链）；见 `sql/notification.sql`、`sql/notification_space_id.sql`。

Auth-required: `GET /api/notification/page`, `GET /api/notification/unread/count`, `PUT /api/notification/{id}/read`, `PUT /api/notification/read/all`.  
Public: `GET /api/picture/{id}`（通知评论/点赞深链打开图片详情）。  
前端通过轮询未读数与列表拉取；无 SSE/WebSocket。

## Picture Like
`picture_like` table stores picture likes (camelCase columns: `id`, `userId`, `pictureId`, `createTime`, `isDelete`). See `sql/picture_like.sql`.  
取消点赞使用**物理删除**（避免软删除占用 `uk_user_picture` 唯一索引导致无法再次点赞）。  
禁止给自己的图片点赞。

Public endpoints (no auth required): `GET /api/picture/{id}/likes`.  
`like/status` 与公共图库 `GET /api/picture/page`、`GET /api/picture/{id}` 走 `OptionalAuthInterceptor`：有合法 token 时识别当前用户并返回/填充是否已赞，未登录 `liked=false`。  
Auth-required: `POST /api/picture/{id}/like`, `DELETE /api/picture/{id}/like`.  
列表与详情 `PictureVO` 含 `likeCount`、`liked`。

## Team Space
`space` / `space_member` / `space_invite` 表存储团队空间与成员（camelCase 列）。见 `sql/space.sql`、`sql/space_member.sql`、`sql/space_invite.sql`。  
角色：`CREATOR`（唯一创建者）、`EDITOR`、`VIEWER`；本阶段只做成员管理，角色对资源的权限矩阵待定。  
退出/踢出成员使用**物理删除**（避免软删除占用 `uk_space_user`）。  
邀请：创建者发起，`userId` 或 `userAccount` 二选一（都传以 `userId` 为准）；待同意后方可加入；邀请角色仅 `EDITOR` / `VIEWER`。  
通知类型 `SPACE_INVITE`；`notification.spaceId` 可空，供深链。取消邀请 / 拒绝 / 解散不删历史通知。  
解散：软删 `space`，物理清成员与 `PENDING` 邀请。

Auth-required（均需登录）：  
`POST /api/space`，`GET /api/space/my`，`GET|PUT|DELETE /api/space/{id}`，  
`GET /api/space/{id}/members`，`PUT /api/space/{id}/members/{userId}/role`，`DELETE /api/space/{id}/members/{userId}`，`DELETE /api/space/{id}/members/me`，  
`POST|GET /api/space/{id}/invites`，`GET /api/space/invites/pending`，  
`POST /api/space/invites/{inviteId}/accept|reject`，`DELETE /api/space/invites/{inviteId}`。

