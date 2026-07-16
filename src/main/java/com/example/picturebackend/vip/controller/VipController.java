package com.example.picturebackend.vip.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.vip.model.dto.VipOrderCreateRequest;
import com.example.picturebackend.vip.model.vo.VipOrderVO;
import com.example.picturebackend.vip.model.vo.VipPlanVO;
import com.example.picturebackend.vip.model.vo.VipStatusVO;
import com.example.picturebackend.vip.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vip")
public class VipController {

    private final VipService vipService;

    public VipController(VipService vipService) {
        this.vipService = vipService;
    }

    @GetMapping("/plans")
    public BaseResponse<List<VipPlanVO>> listPlans(HttpServletRequest request) {
        requireLoginUserId(request);
        return ResultUtils.success(vipService.listPlans());
    }

    @GetMapping("/status")
    public BaseResponse<VipStatusVO> status(HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(vipService.getStatus(userId));
    }

    @PostMapping("/orders")
    public BaseResponse<VipOrderVO> createOrder(@RequestBody VipOrderCreateRequest body, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(vipService.createOrder(body, userId));
    }

    @PostMapping("/orders/{orderNo}/mock-pay")
    public BaseResponse<VipOrderVO> mockPay(@PathVariable String orderNo, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(vipService.mockPay(orderNo, userId));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public BaseResponse<VipOrderVO> cancel(@PathVariable String orderNo, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(vipService.cancelOrder(orderNo, userId));
    }

    @GetMapping("/orders")
    public BaseResponse<IPage<VipOrderVO>> pageOrders(PageRequest pageRequest, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(vipService.pageMyOrders(userId, pageRequest));
    }

    private Long requireLoginUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return userId;
    }
}
