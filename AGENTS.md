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
`notification` table stores in-app notifications (camelCase columns: `id`, `receiverId`, `senderId`, `type`, `pictureId`, `commentId`, `content`, `isRead`, `createTime`, `isDelete`). See `sql/notification.sql`.

Types: `FOLLOW`（有人关注我）、`COMMENT`（有人评论我的图片）、`REPLY`（有人回复我的评论）。  
写入规则：不通知自己；回复时若图片作者与父评论作者为同一人，只写一条且优先 `REPLY`。  
关注成功、发表评论后与业务同事务写入；取消关注 / 删除评论不删除历史通知。

Auth-required: `GET /api/notification/page`, `GET /api/notification/unread/count`, `PUT /api/notification/{id}/read`, `PUT /api/notification/read/all`.  
Public: `GET /api/picture/{id}`（通知评论深链打开图片详情）。  
前端通过轮询未读数与列表拉取；无 SSE/WebSocket。

