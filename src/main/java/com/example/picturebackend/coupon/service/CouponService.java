package com.example.picturebackend.coupon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.coupon.entity.UserCoupon;
import com.example.picturebackend.coupon.model.dto.CouponActivityCreateRequest;
import com.example.picturebackend.coupon.model.dto.CouponActivityUpdateRequest;
import com.example.picturebackend.coupon.model.vo.CouponActivityVO;
import com.example.picturebackend.coupon.model.vo.UserCouponVO;

import java.util.List;

public interface CouponService {

    List<CouponActivityVO> listOnShelfActivities(Long userId);

    CouponActivityVO getActivity(Long activityId, Long userId);

    UserCouponVO claim(Long activityId, Long userId);

    IPage<UserCouponVO> pageMyCoupons(Long userId, String status, PageRequest pageRequest);

    IPage<CouponActivityVO> pageAdminActivities(PageRequest pageRequest);

    CouponActivityVO createActivity(CouponActivityCreateRequest request);

    CouponActivityVO updateActivity(Long activityId, CouponActivityUpdateRequest request);

    /** 校验归属与 UNUSED（含惰性过期），返回可用券 */
    UserCoupon requireUsableCoupon(Long couponId, Long userId);

    void lockCoupon(Long couponId, Long userId, String orderNo);

    void unlockCouponForOrder(Long couponId, String orderNo);

    void markCouponUsed(Long couponId, String orderNo);
}
