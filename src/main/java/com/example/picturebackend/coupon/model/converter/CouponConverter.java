package com.example.picturebackend.coupon.model.converter;

import com.example.picturebackend.coupon.entity.CouponActivity;
import com.example.picturebackend.coupon.entity.UserCoupon;
import com.example.picturebackend.coupon.model.vo.CouponActivityVO;
import com.example.picturebackend.coupon.model.vo.UserCouponVO;

public final class CouponConverter {

    private CouponConverter() {
    }

    public static CouponActivityVO toActivityVO(CouponActivity activity,
                                                Integer remainStock,
                                                boolean claimed,
                                                boolean ongoing) {
        if (activity == null) {
            return null;
        }
        CouponActivityVO vo = new CouponActivityVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setDiscountCents(activity.getDiscountCents());
        vo.setTotalStock(activity.getTotalStock());
        vo.setRemainStock(remainStock);
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setCouponValidDays(activity.getCouponValidDays());
        vo.setStatus(activity.getStatus());
        vo.setOngoing(ongoing);
        vo.setClaimed(claimed);
        return vo;
    }

    public static UserCouponVO toUserCouponVO(UserCoupon coupon, String activityName) {
        if (coupon == null) {
            return null;
        }
        UserCouponVO vo = new UserCouponVO();
        vo.setId(coupon.getId());
        vo.setActivityId(coupon.getActivityId());
        vo.setActivityName(activityName);
        vo.setDiscountCents(coupon.getDiscountCents());
        vo.setStatus(coupon.getStatus());
        vo.setExpireTime(coupon.getExpireTime());
        vo.setLockOrderNo(coupon.getLockOrderNo());
        vo.setCreateTime(coupon.getCreateTime());
        return vo;
    }
}
