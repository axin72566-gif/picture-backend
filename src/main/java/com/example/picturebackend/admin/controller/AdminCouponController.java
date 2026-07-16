package com.example.picturebackend.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.admin.AdminAuth;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.coupon.model.dto.CouponActivityCreateRequest;
import com.example.picturebackend.coupon.model.dto.CouponActivityUpdateRequest;
import com.example.picturebackend.coupon.model.vo.CouponActivityVO;
import com.example.picturebackend.coupon.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupon")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/activities")
    public BaseResponse<IPage<CouponActivityVO>> pageActivities(PageRequest pageRequest,
                                                                HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        return ResultUtils.success(couponService.pageAdminActivities(pageRequest));
    }

    @PostMapping("/activities")
    public BaseResponse<CouponActivityVO> createActivity(@RequestBody CouponActivityCreateRequest body,
                                                         HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        return ResultUtils.success(couponService.createActivity(body));
    }

    @PutMapping("/activities/{id}")
    public BaseResponse<CouponActivityVO> updateActivity(@PathVariable("id") Long id,
                                                         @RequestBody CouponActivityUpdateRequest body,
                                                         HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        return ResultUtils.success(couponService.updateActivity(id, body));
    }
}
