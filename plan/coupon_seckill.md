# 优惠券秒杀（固定金额抵扣 VIP）

## 选定方案

- **范围**：活动配置 → Redis 秒杀领券 → 我的券 → VIP 下单抵扣 → 支付/取消/过期与券状态联动
- **券类型**：仅 **固定金额减免**（`discountCents`，单位分）；`amountCents = max(0, priceCents - discountCents)`，允许实付 0
- **并发**：Redis 预热库存 + Lua 原子扣减；DB `uk_activity_user` 兜底防超领
- **限领**：每用户每活动 1 张
- **用券**：一笔订单最多 1 张；创建时 `UNUSED → LOCKED`，支付 `USED`，取消/惰性过期订单 `LOCKED → UNUSED`（若券本身已过期则 `EXPIRED`）
- **管理**：`userRole=admin` 创建/更新/上架并预热 Redis

## 数据模型

- [`sql/coupon_activity.sql`](../sql/coupon_activity.sql)：活动 + 演示种子
- [`sql/user_coupon.sql`](../sql/user_coupon.sql)：用户券；状态 `UNUSED` / `LOCKED` / `USED` / `EXPIRED`
- [`sql/vip_order_coupon.sql`](../sql/vip_order_coupon.sql) / [`sql/vip_order.sql`](../sql/vip_order.sql)：`originalAmountCents`、`discountCents`、`couponId`

券过期：读列表 / 用券前惰性刷新（无定时任务）。

## Redis

- `coupon:seckill:{activityId}:stock`
- `coupon:seckill:{activityId}:users`

上架或库存 key 缺失时：`stock = totalStock - claimedCount`。领取 Lua：已领 / 售罄 / DECR+SADD；DB 失败则 INCR+SREM 补偿。

## API（均需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/coupon/activities` | 上架活动（进行中优先） |
| GET | `/api/coupon/activities/{id}` | 详情 + 库存 + 是否已领 |
| POST | `/api/coupon/activities/{id}/claim` | 秒杀领券 |
| GET | `/api/coupon/mine` | 我的券分页（可选 `status`） |
| GET | `/api/admin/coupon/activities` | 管理端分页 |
| POST | `/api/admin/coupon/activities` | 创建（可选立即上架） |
| PUT | `/api/admin/coupon/activities/{id}` | 更新/上下架；上架预热 Redis |

VIP：`POST /api/vip/orders` 可选 `couponId`；订单 VO 含原价/减免/券 ID。

## 错误码

`42101`–`42108`：活动不存在/未开始/已结束、售罄、已领、券不存在/状态错误/已过期。

## 包

`com.example.picturebackend.coupon`；管理入口 `AdminCouponController`（`/api/admin/coupon`）。
