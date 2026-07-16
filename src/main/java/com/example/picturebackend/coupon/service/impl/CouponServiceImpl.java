package com.example.picturebackend.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.coupon.constant.CouponConstant;
import com.example.picturebackend.coupon.constant.UserCouponStatus;
import com.example.picturebackend.coupon.entity.CouponActivity;
import com.example.picturebackend.coupon.entity.UserCoupon;
import com.example.picturebackend.coupon.mapper.CouponActivityMapper;
import com.example.picturebackend.coupon.mapper.UserCouponMapper;
import com.example.picturebackend.coupon.model.converter.CouponConverter;
import com.example.picturebackend.coupon.model.dto.CouponActivityCreateRequest;
import com.example.picturebackend.coupon.model.dto.CouponActivityUpdateRequest;
import com.example.picturebackend.coupon.model.vo.CouponActivityVO;
import com.example.picturebackend.coupon.model.vo.UserCouponVO;
import com.example.picturebackend.coupon.service.CouponService;
import com.example.picturebackend.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl implements CouponService {

    private static final DefaultRedisScript<Long> CLAIM_SCRIPT = new DefaultRedisScript<>();

    static {
        CLAIM_SCRIPT.setResultType(Long.class);
        CLAIM_SCRIPT.setScriptText("""
                if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                  return -1
                end
                local stock = redis.call('GET', KEYS[1])
                if (not stock) or (tonumber(stock) <= 0) then
                  return -2
                end
                redis.call('DECR', KEYS[1])
                redis.call('SADD', KEYS[2], ARGV[1])
                return 1
                """);
    }

    private final CouponActivityMapper couponActivityMapper;

    private final UserCouponMapper userCouponMapper;

    private final StringRedisTemplate stringRedisTemplate;

    public CouponServiceImpl(CouponActivityMapper couponActivityMapper,
                             UserCouponMapper userCouponMapper,
                             StringRedisTemplate stringRedisTemplate) {
        this.couponActivityMapper = couponActivityMapper;
        this.userCouponMapper = userCouponMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<CouponActivityVO> listOnShelfActivities(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<CouponActivity> activities = couponActivityMapper.selectList(new LambdaQueryWrapper<CouponActivity>()
                .eq(CouponActivity::getStatus, CouponConstant.ACTIVITY_STATUS_ON)
                .eq(CouponActivity::getIsDelete, 0)
                .orderByDesc(CouponActivity::getStartTime));
        LocalDateTime now = LocalDateTime.now();
        return activities.stream()
                .sorted((a, b) -> Boolean.compare(isOngoing(b, now), isOngoing(a, now)))
                .map(a -> toActivityVO(a, userId))
                .toList();
    }

    private static boolean isOngoing(CouponActivity activity, LocalDateTime now) {
        return activity.getStartTime() != null
                && activity.getEndTime() != null
                && !now.isBefore(activity.getStartTime())
                && now.isBefore(activity.getEndTime());
    }

    @Override
    public CouponActivityVO getActivity(Long activityId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        CouponActivity activity = requireActivity(activityId);
        if (!Objects.equals(activity.getStatus(), CouponConstant.ACTIVITY_STATUS_ON)) {
            throw new BusinessException(ErrorCode.COUPON_ACTIVITY_NOT_FOUND);
        }
        return toActivityVO(activity, userId);
    }

    @Override
    @Transactional
    public UserCouponVO claim(Long activityId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (activityId == null || activityId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动ID无效");
        }
        CouponActivity activity = requireActivity(activityId);
        assertClaimableWindow(activity);
        ensureRedisStock(activity);

        Long luaResult = stringRedisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(CouponConstant.stockKey(activityId), CouponConstant.usersKey(activityId)),
                String.valueOf(userId));
        if (luaResult == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "领取失败，请稍后重试");
        }
        if (luaResult == CouponConstant.CLAIM_RESULT_ALREADY) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_CLAIMED);
        }
        if (luaResult == CouponConstant.CLAIM_RESULT_SOLD_OUT) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        LocalDateTime now = LocalDateTime.now();
        UserCoupon coupon = new UserCoupon();
        coupon.setActivityId(activityId);
        coupon.setUserId(userId);
        coupon.setDiscountCents(activity.getDiscountCents());
        coupon.setStatus(UserCouponStatus.UNUSED);
        coupon.setExpireTime(now.plusDays(activity.getCouponValidDays()));

        try {
            int rows = userCouponMapper.insert(coupon);
            if (rows <= 0 || coupon.getId() == null) {
                rollbackRedisClaim(activityId, userId);
                throw new BusinessException(ErrorCode.SERVER_ERROR, "领取失败");
            }
        } catch (DuplicateKeyException ex) {
            rollbackRedisClaim(activityId, userId);
            throw new BusinessException(ErrorCode.COUPON_ALREADY_CLAIMED);
        } catch (RuntimeException ex) {
            rollbackRedisClaim(activityId, userId);
            throw ex;
        }

        couponActivityMapper.update(null, new LambdaUpdateWrapper<CouponActivity>()
                .eq(CouponActivity::getId, activityId)
                .setSql("claimedCount = claimedCount + 1"));

        return CouponConverter.toUserCouponVO(coupon, activity.getName());
    }

    @Override
    public IPage<UserCouponVO> pageMyCoupons(Long userId, String status, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        expireUnusedCouponsForUser(userId);

        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<UserCoupon> page = new Page<>(req.getCurrent(), req.getPageSize());
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getIsDelete, 0)
                .orderByDesc(UserCoupon::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserCoupon::getStatus, status.trim());
        }
        Page<UserCoupon> couponPage = userCouponMapper.selectPage(page, wrapper);
        List<UserCoupon> records = couponPage.getRecords();
        if (records == null || records.isEmpty()) {
            IPage<UserCouponVO> empty = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
            empty.setRecords(List.of());
            return empty;
        }

        Set<Long> activityIds = records.stream().map(UserCoupon::getActivityId).collect(Collectors.toSet());
        Map<Long, CouponActivity> activityMap = couponActivityMapper.selectBatchIds(activityIds).stream()
                .collect(Collectors.toMap(CouponActivity::getId, Function.identity(), (a, b) -> a));

        List<UserCouponVO> voList = records.stream()
                .map(c -> {
                    CouponActivity a = activityMap.get(c.getActivityId());
                    return CouponConverter.toUserCouponVO(c, a != null ? a.getName() : null);
                })
                .toList();
        IPage<UserCouponVO> result = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public IPage<CouponActivityVO> pageAdminActivities(PageRequest pageRequest) {
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<CouponActivity> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<CouponActivity> activityPage = couponActivityMapper.selectPage(page, new LambdaQueryWrapper<CouponActivity>()
                .eq(CouponActivity::getIsDelete, 0)
                .orderByDesc(CouponActivity::getId));
        List<CouponActivityVO> voList = activityPage.getRecords().stream()
                .map(a -> toActivityVO(a, null))
                .toList();
        IPage<CouponActivityVO> result = new Page<>(activityPage.getCurrent(), activityPage.getSize(), activityPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional
    public CouponActivityVO createActivity(CouponActivityCreateRequest request) {
        validateActivityRequest(request.getName(), request.getDiscountCents(), request.getTotalStock(),
                request.getStartTime(), request.getEndTime(), request.getCouponValidDays());

        CouponActivity activity = new CouponActivity();
        activity.setName(request.getName().trim());
        activity.setDiscountCents(request.getDiscountCents());
        activity.setTotalStock(request.getTotalStock());
        activity.setClaimedCount(0);
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCouponValidDays(request.getCouponValidDays());
        boolean online = Boolean.TRUE.equals(request.getOnline());
        activity.setStatus(online ? CouponConstant.ACTIVITY_STATUS_ON : CouponConstant.ACTIVITY_STATUS_OFF);

        int rows = couponActivityMapper.insert(activity);
        if (rows <= 0 || activity.getId() == null) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建活动失败");
        }
        if (online) {
            warmRedisStock(activity);
        }
        return toActivityVO(activity, null);
    }

    @Override
    @Transactional
    public CouponActivityVO updateActivity(Long activityId, CouponActivityUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CouponActivity activity = requireActivity(activityId);

        if (StringUtils.hasText(request.getName())) {
            activity.setName(request.getName().trim());
        }
        if (request.getDiscountCents() != null) {
            if (request.getDiscountCents() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "减免金额须大于0");
            }
            activity.setDiscountCents(request.getDiscountCents());
        }
        if (request.getTotalStock() != null) {
            int claimed = activity.getClaimedCount() == null ? 0 : activity.getClaimedCount();
            if (request.getTotalStock() < claimed) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "总库存不能小于已领取数");
            }
            activity.setTotalStock(request.getTotalStock());
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        if (request.getCouponValidDays() != null) {
            if (request.getCouponValidDays() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "有效天数须大于0");
            }
            activity.setCouponValidDays(request.getCouponValidDays());
        }
        if (activity.getEndTime() != null && activity.getStartTime() != null
                && !activity.getEndTime().isAfter(activity.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间须晚于开始时间");
        }

        Integer prevStatus = activity.getStatus();
        if (request.getStatus() != null) {
            if (!Objects.equals(request.getStatus(), CouponConstant.ACTIVITY_STATUS_ON)
                    && !Objects.equals(request.getStatus(), CouponConstant.ACTIVITY_STATUS_OFF)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态无效");
            }
            activity.setStatus(request.getStatus());
        }

        int rows = couponActivityMapper.updateById(activity);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "更新活动失败");
        }

        if (Objects.equals(activity.getStatus(), CouponConstant.ACTIVITY_STATUS_ON)) {
            // 上架或已上架改库存：校准 Redis（仅当 key 不存在时预热；改 totalStock 时重写剩余）
            if (!Objects.equals(prevStatus, CouponConstant.ACTIVITY_STATUS_ON) || request.getTotalStock() != null) {
                warmRedisStock(activity);
            } else {
                ensureRedisStock(activity);
            }
        }

        return toActivityVO(activity, null);
    }

    @Override
    public UserCoupon requireUsableCoupon(Long couponId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (couponId == null || couponId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优惠券无效");
        }
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || Objects.equals(coupon.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        if (!Objects.equals(coupon.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能使用自己的优惠券");
        }
        refreshExpiredIfNeeded(coupon);
        if (UserCouponStatus.EXPIRED.equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        if (!UserCouponStatus.UNUSED.equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_STATUS_ERROR);
        }
        return coupon;
    }

    @Override
    @Transactional
    public void lockCoupon(Long couponId, Long userId, String orderNo) {
        requireUsableCoupon(couponId, userId);
        int rows = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .set(UserCoupon::getStatus, UserCouponStatus.LOCKED)
                .set(UserCoupon::getLockOrderNo, orderNo));
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.COUPON_STATUS_ERROR, "锁定优惠券失败");
        }
    }

    @Override
    @Transactional
    public void unlockCouponForOrder(Long couponId, String orderNo) {
        if (couponId == null || !StringUtils.hasText(orderNo)) {
            return;
        }
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || Objects.equals(coupon.getIsDelete(), 1)) {
            return;
        }
        if (!UserCouponStatus.LOCKED.equals(coupon.getStatus())) {
            return;
        }
        if (!Objects.equals(orderNo, coupon.getLockOrderNo())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String nextStatus = (coupon.getExpireTime() != null && !coupon.getExpireTime().isAfter(now))
                ? UserCouponStatus.EXPIRED
                : UserCouponStatus.UNUSED;
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getStatus, UserCouponStatus.LOCKED)
                .eq(UserCoupon::getLockOrderNo, orderNo)
                .set(UserCoupon::getStatus, nextStatus)
                .set(UserCoupon::getLockOrderNo, null));
    }

    @Override
    @Transactional
    public void markCouponUsed(Long couponId, String orderNo) {
        if (couponId == null || !StringUtils.hasText(orderNo)) {
            return;
        }
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || Objects.equals(coupon.getIsDelete(), 1)) {
            return;
        }
        if (UserCouponStatus.USED.equals(coupon.getStatus())
                && Objects.equals(orderNo, coupon.getLockOrderNo())) {
            return;
        }
        if (!UserCouponStatus.LOCKED.equals(coupon.getStatus())
                || !Objects.equals(orderNo, coupon.getLockOrderNo())) {
            throw new BusinessException(ErrorCode.COUPON_STATUS_ERROR, "优惠券未正确锁定");
        }
        coupon.setStatus(UserCouponStatus.USED);
        int rows = userCouponMapper.updateById(coupon);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "核销优惠券失败");
        }
    }

    private CouponActivityVO toActivityVO(CouponActivity activity, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        boolean ongoing = Objects.equals(activity.getStatus(), CouponConstant.ACTIVITY_STATUS_ON)
                && activity.getStartTime() != null
                && activity.getEndTime() != null
                && !now.isBefore(activity.getStartTime())
                && now.isBefore(activity.getEndTime());
        int remain = resolveRemainStock(activity);
        boolean claimed = false;
        if (userId != null) {
            Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getActivityId, activity.getId())
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getIsDelete, 0));
            claimed = count != null && count > 0;
        }
        return CouponConverter.toActivityVO(activity, remain, claimed, ongoing);
    }

    private int resolveRemainStock(CouponActivity activity) {
        String stock = stringRedisTemplate.opsForValue().get(CouponConstant.stockKey(activity.getId()));
        if (stock != null) {
            try {
                return Math.max(0, Integer.parseInt(stock));
            } catch (NumberFormatException ignored) {
                // fall through to DB
            }
        }
        int claimed = activity.getClaimedCount() == null ? 0 : activity.getClaimedCount();
        int total = activity.getTotalStock() == null ? 0 : activity.getTotalStock();
        return Math.max(0, total - claimed);
    }

    private void assertClaimableWindow(CouponActivity activity) {
        if (!Objects.equals(activity.getStatus(), CouponConstant.ACTIVITY_STATUS_ON)) {
            throw new BusinessException(ErrorCode.COUPON_ACTIVITY_NOT_FOUND, "活动未上架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BusinessException(ErrorCode.COUPON_ACTIVITY_NOT_STARTED);
        }
        if (activity.getEndTime() != null && !now.isBefore(activity.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_ACTIVITY_ENDED);
        }
    }

    private CouponActivity requireActivity(Long activityId) {
        if (activityId == null || activityId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动ID无效");
        }
        CouponActivity activity = couponActivityMapper.selectById(activityId);
        if (activity == null || Objects.equals(activity.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.COUPON_ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    private void ensureRedisStock(CouponActivity activity) {
        Boolean hasKey = stringRedisTemplate.hasKey(CouponConstant.stockKey(activity.getId()));
        if (!Boolean.TRUE.equals(hasKey)) {
            warmRedisStock(activity);
        }
    }

    private void warmRedisStock(CouponActivity activity) {
        int claimed = activity.getClaimedCount() == null ? 0 : activity.getClaimedCount();
        int total = activity.getTotalStock() == null ? 0 : activity.getTotalStock();
        int remain = Math.max(0, total - claimed);
        stringRedisTemplate.opsForValue().set(CouponConstant.stockKey(activity.getId()), String.valueOf(remain));
    }

    private void rollbackRedisClaim(Long activityId, Long userId) {
        stringRedisTemplate.opsForValue().increment(CouponConstant.stockKey(activityId));
        stringRedisTemplate.opsForSet().remove(CouponConstant.usersKey(activityId), String.valueOf(userId));
    }

    private void validateActivityRequest(String name, Integer discountCents, Integer totalStock,
                                         LocalDateTime startTime, LocalDateTime endTime, Integer couponValidDays) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动名称不能为空");
        }
        if (discountCents == null || discountCents <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "减免金额须大于0");
        }
        if (totalStock == null || totalStock <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "库存须大于0");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请设置活动时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间须晚于开始时间");
        }
        if (couponValidDays == null || couponValidDays <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "有效天数须大于0");
        }
    }

    private void expireUnusedCouponsForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> expired = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .eq(UserCoupon::getIsDelete, 0)
                .le(UserCoupon::getExpireTime, now));
        for (UserCoupon coupon : expired) {
            coupon.setStatus(UserCouponStatus.EXPIRED);
            userCouponMapper.updateById(coupon);
        }
    }

    private void refreshExpiredIfNeeded(UserCoupon coupon) {
        if (coupon == null || !UserCouponStatus.UNUSED.equals(coupon.getStatus())) {
            return;
        }
        if (coupon.getExpireTime() != null && !coupon.getExpireTime().isAfter(LocalDateTime.now())) {
            coupon.setStatus(UserCouponStatus.EXPIRED);
            userCouponMapper.updateById(coupon);
        }
    }
}
