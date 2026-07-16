package com.example.picturebackend.coupon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.coupon.model.vo.CouponActivityVO;
import com.example.picturebackend.coupon.model.vo.UserCouponVO;
import com.example.picturebackend.coupon.service.CouponService;
import com.example.picturebackend.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/activities")
    public BaseResponse<List<CouponActivityVO>> listActivities(HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(couponService.listOnShelfActivities(userId));
    }

    @GetMapping("/activities/{id}")
    public BaseResponse<CouponActivityVO> getActivity(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(couponService.getActivity(id, userId));
    }

    @PostMapping("/activities/{id}/claim")
    public BaseResponse<UserCouponVO> claim(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(couponService.claim(id, userId));
    }

    @GetMapping("/mine")
    public BaseResponse<IPage<UserCouponVO>> pageMine(@RequestParam(required = false) String status,
                                                      PageRequest pageRequest,
                                                      HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(couponService.pageMyCoupons(userId, status, pageRequest));
    }

    private Long requireLoginUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return userId;
    }
}
