# VIP 购买（团队空间额度）

## 选定方案

- **范围**：套餐列表 → 下单 → **模拟支付** → 开通/续期；空间创建与邀请校验额度
- **支付**：不接微信/支付宝；`POST /api/vip/orders/{orderNo}/mock-pay` 模拟成功
- **支付时限**：下单后 **15 分钟**；超时订单置为 `EXPIRED`（惰性过期，无定时扫表）
- **权益**：`user.vipExpireTime` 有效期内提升可创建空间数与单空间成员上限（与 `userRole` 正交）

| | 免费 | VIP 有效期内 |
|--|------|--------------|
| 本人作为 CREATOR 的空间数 | 1 | 5 |
| 单空间成员数（含创建者） | 5 | 50 |

成员上限以该空间 **CREATOR（ownerId）** 是否 VIP 为准。  
VIP 过期后不强制缩减已有超限空间，仅拦截新建空间 / 新邀请 / 接受导致超员。

## 数据模型

- [`sql/user_vip.sql`](../sql/user_vip.sql) / [`sql/user.sql`](../sql/user.sql)：`vipExpireTime`
- [`sql/vip_plan.sql`](../sql/vip_plan.sql)：套餐 + 种子 `MONTH` / `QUARTER` / `YEAR`
- [`sql/vip_order.sql`](../sql/vip_order.sql)：订单；状态 `PENDING` / `PAID` / `CANCELLED` / `EXPIRED`；`expireTime`

开通：`newExpire = max(now, 当前 vipExpireTime) + durationDays`。

## API（均需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/vip/plans` | 上架套餐 |
| `GET` | `/api/vip/status` | VIP 状态 + 空间额度用量 |
| `POST` | `/api/vip/orders` | 创建订单（15min 支付窗）；同用户仅一笔未过期 PENDING |
| `POST` | `/api/vip/orders/{orderNo}/mock-pay` | 模拟支付（幂等） |
| `POST` | `/api/vip/orders/{orderNo}/cancel` | 取消待支付订单 |
| `GET` | `/api/vip/orders` | 我的订单分页 |

`UserVO` 含 `vipExpireTime`、`vipActive`。  
下单可选 `couponId` 固定金额抵扣；订单含 `originalAmountCents` / `discountCents` / `couponId`。见 [`plan/coupon_seckill.md`](coupon_seckill.md)。

## 额度落点

- `SpaceServiceImpl.createSpace` → `VipQuotaService.requireCanCreateSpace`
- `SpaceInviteServiceImpl.invite` → `requireCanInviteMember`（成员 + PENDING）
- `SpaceInviteServiceImpl.accept` → `requireCanAcceptMember`（仅成员数；该笔 PENDING 已占名额）

## 包

`com.example.picturebackend.vip`：entity / mapper / constant / service / controller。
