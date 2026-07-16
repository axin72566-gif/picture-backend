package com.example.picturebackend.vip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.coupon.entity.UserCoupon;
import com.example.picturebackend.coupon.service.CouponService;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.mapper.SpaceMapper;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.vip.constant.VipConstant;
import com.example.picturebackend.vip.constant.VipOrderStatus;
import com.example.picturebackend.vip.entity.VipOrder;
import com.example.picturebackend.vip.entity.VipPlan;
import com.example.picturebackend.vip.mapper.VipOrderMapper;
import com.example.picturebackend.vip.mapper.VipPlanMapper;
import com.example.picturebackend.vip.model.converter.VipConverter;
import com.example.picturebackend.vip.model.dto.VipOrderCreateRequest;
import com.example.picturebackend.vip.model.vo.VipOrderVO;
import com.example.picturebackend.vip.model.vo.VipPlanVO;
import com.example.picturebackend.vip.model.vo.VipStatusVO;
import com.example.picturebackend.vip.service.VipQuotaService;
import com.example.picturebackend.vip.service.VipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VipServiceImpl implements VipService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VipPlanMapper vipPlanMapper;

    private final VipOrderMapper vipOrderMapper;

    private final UserMapper userMapper;

    private final SpaceMapper spaceMapper;

    private final VipQuotaService vipQuotaService;

    private final CouponService couponService;

    public VipServiceImpl(VipPlanMapper vipPlanMapper,
                          VipOrderMapper vipOrderMapper,
                          UserMapper userMapper,
                          SpaceMapper spaceMapper,
                          VipQuotaService vipQuotaService,
                          CouponService couponService) {
        this.vipPlanMapper = vipPlanMapper;
        this.vipOrderMapper = vipOrderMapper;
        this.userMapper = userMapper;
        this.spaceMapper = spaceMapper;
        this.vipQuotaService = vipQuotaService;
        this.couponService = couponService;
    }

    @Override
    public List<VipPlanVO> listPlans() {
        List<VipPlan> plans = vipPlanMapper.selectList(new LambdaQueryWrapper<VipPlan>()
                .eq(VipPlan::getStatus, VipConstant.PLAN_STATUS_ON)
                .eq(VipPlan::getIsDelete, 0)
                .orderByAsc(VipPlan::getDurationDays));
        return plans.stream().map(VipConverter::toPlanVO).toList();
    }

    @Override
    public VipStatusVO getStatus(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        boolean active = VipQuotaServiceImpl.isVipActive(user);
        Long ownedCount = spaceMapper.selectCount(new LambdaQueryWrapper<Space>()
                .eq(Space::getOwnerId, userId)
                .eq(Space::getIsDelete, 0));

        VipStatusVO vo = new VipStatusVO();
        vo.setVipActive(active);
        vo.setVipExpireTime(user.getVipExpireTime());
        vo.setMaxOwnedSpaces(vipQuotaService.maxOwnedSpaces(userId));
        vo.setOwnedSpaceCount(ownedCount == null ? 0 : ownedCount.intValue());
        vo.setMaxMembersPerSpace(vipQuotaService.maxMembersPerSpace(userId));
        return vo;
    }

    @Override
    @Transactional
    public VipOrderVO createOrder(VipOrderCreateRequest request, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (request == null || request.getPlanId() == null || request.getPlanId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择套餐");
        }
        User user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        expirePendingOrdersForUser(userId);

        Long activePending = vipOrderMapper.selectCount(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getUserId, userId)
                .eq(VipOrder::getStatus, VipOrderStatus.PENDING)
                .eq(VipOrder::getIsDelete, 0));
        if (activePending != null && activePending > 0) {
            throw new BusinessException(ErrorCode.VIP_PENDING_ORDER_EXISTS, "已有待支付订单，请先支付或取消");
        }

        VipPlan plan = requireOnShelfPlan(request.getPlanId());
        LocalDateTime now = LocalDateTime.now();

        int originalCents = plan.getPriceCents() == null ? 0 : plan.getPriceCents();
        int discountCents = 0;
        Long couponId = request.getCouponId();
        String orderNo = generateOrderNo(userId);

        if (couponId != null) {
            UserCoupon coupon = couponService.requireUsableCoupon(couponId, userId);
            discountCents = coupon.getDiscountCents() == null ? 0 : coupon.getDiscountCents();
            couponService.lockCoupon(couponId, userId, orderNo);
        }

        int amountCents = Math.max(0, originalCents - discountCents);

        VipOrder order = new VipOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setDurationDays(plan.getDurationDays());
        order.setOriginalAmountCents(originalCents);
        order.setDiscountCents(discountCents);
        order.setCouponId(couponId);
        order.setAmountCents(amountCents);
        order.setStatus(VipOrderStatus.PENDING);
        order.setExpireTime(now.plusMinutes(VipConstant.PAY_TIMEOUT_MINUTES));
        int rows = vipOrderMapper.insert(order);
        if (rows <= 0 || order.getId() == null) {
            if (couponId != null) {
                couponService.unlockCouponForOrder(couponId, orderNo);
            }
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建订单失败");
        }
        return VipConverter.toOrderVO(order, plan);
    }

    @Override
    @Transactional
    public VipOrderVO mockPay(String orderNo, Long userId) {
        VipOrder order = requireOwnOrder(orderNo, userId);
        if (VipOrderStatus.PAID.equals(order.getStatus())) {
            VipPlan plan = vipPlanMapper.selectById(order.getPlanId());
            return VipConverter.toOrderVO(order, plan);
        }
        refreshExpiredIfNeeded(order);
        if (VipOrderStatus.EXPIRED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.VIP_ORDER_EXPIRED);
        }
        if (!VipOrderStatus.PENDING.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.VIP_ORDER_STATUS_ERROR);
        }
        if (!order.getExpireTime().isAfter(LocalDateTime.now())) {
            markExpired(order);
            throw new BusinessException(ErrorCode.VIP_ORDER_EXPIRED);
        }

        LocalDateTime paidAt = LocalDateTime.now();
        order.setStatus(VipOrderStatus.PAID);
        order.setPaidTime(paidAt);
        int orderRows = vipOrderMapper.updateById(order);
        if (orderRows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "支付失败");
        }

        if (order.getCouponId() != null) {
            couponService.markCouponUsed(order.getCouponId(), order.getOrderNo());
        }

        User user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDateTime base = user.getVipExpireTime();
        if (base == null || !base.isAfter(paidAt)) {
            base = paidAt;
        }
        user.setVipExpireTime(base.plusDays(order.getDurationDays()));
        int userRows = userMapper.updateById(user);
        if (userRows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "开通 VIP 失败");
        }

        VipPlan plan = vipPlanMapper.selectById(order.getPlanId());
        return VipConverter.toOrderVO(order, plan);
    }

    @Override
    @Transactional
    public VipOrderVO cancelOrder(String orderNo, Long userId) {
        VipOrder order = requireOwnOrder(orderNo, userId);
        refreshExpiredIfNeeded(order);
        if (VipOrderStatus.EXPIRED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.VIP_ORDER_EXPIRED, "订单已过期，无法取消");
        }
        if (!VipOrderStatus.PENDING.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.VIP_ORDER_STATUS_ERROR, "仅待支付订单可取消");
        }
        order.setStatus(VipOrderStatus.CANCELLED);
        int rows = vipOrderMapper.updateById(order);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "取消订单失败");
        }
        if (order.getCouponId() != null) {
            couponService.unlockCouponForOrder(order.getCouponId(), order.getOrderNo());
        }
        VipPlan plan = vipPlanMapper.selectById(order.getPlanId());
        return VipConverter.toOrderVO(order, plan);
    }

    @Override
    @Transactional
    public IPage<VipOrderVO> pageMyOrders(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        expirePendingOrdersForUser(userId);

        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<VipOrder> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<VipOrder> orderPage = vipOrderMapper.selectPage(page, new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getUserId, userId)
                .eq(VipOrder::getIsDelete, 0)
                .orderByDesc(VipOrder::getId));

        List<VipOrder> records = orderPage.getRecords();
        if (records == null || records.isEmpty()) {
            IPage<VipOrderVO> empty = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
            empty.setRecords(List.of());
            return empty;
        }

        Set<Long> planIds = records.stream().map(VipOrder::getPlanId).collect(Collectors.toSet());
        Map<Long, VipPlan> planMap = vipPlanMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(VipPlan::getId, Function.identity(), (a, b) -> a));

        List<VipOrderVO> voList = records.stream()
                .map(o -> VipConverter.toOrderVO(o, planMap.get(o.getPlanId())))
                .toList();
        IPage<VipOrderVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    private VipPlan requireOnShelfPlan(Long planId) {
        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null || Objects.equals(plan.getIsDelete(), 1)
                || !Objects.equals(plan.getStatus(), VipConstant.PLAN_STATUS_ON)) {
            throw new BusinessException(ErrorCode.VIP_PLAN_NOT_FOUND);
        }
        return plan;
    }

    private VipOrder requireOwnOrder(String orderNo, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单号不能为空");
        }
        VipOrder order = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo.trim())
                .eq(VipOrder::getIsDelete, 0)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.VIP_ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能操作自己的订单");
        }
        return order;
    }

    private void expirePendingOrdersForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<VipOrder> pending = vipOrderMapper.selectList(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getUserId, userId)
                .eq(VipOrder::getStatus, VipOrderStatus.PENDING)
                .eq(VipOrder::getIsDelete, 0)
                .le(VipOrder::getExpireTime, now));
        for (VipOrder order : pending) {
            markExpired(order);
        }
    }

    private void refreshExpiredIfNeeded(VipOrder order) {
        if (order == null || !VipOrderStatus.PENDING.equals(order.getStatus())) {
            return;
        }
        if (order.getExpireTime() != null && !order.getExpireTime().isAfter(LocalDateTime.now())) {
            markExpired(order);
        }
    }

    private void markExpired(VipOrder order) {
        order.setStatus(VipOrderStatus.EXPIRED);
        vipOrderMapper.updateById(order);
        if (order.getCouponId() != null) {
            couponService.unlockCouponForOrder(order.getCouponId(), order.getOrderNo());
        }
    }

    private static String generateOrderNo(Long userId) {
        int rand = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "VIP" + LocalDateTime.now().format(ORDER_NO_TIME) + userId + rand;
    }
}
