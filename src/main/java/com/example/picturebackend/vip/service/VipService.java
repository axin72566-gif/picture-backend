package com.example.picturebackend.vip.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.vip.model.dto.VipOrderCreateRequest;
import com.example.picturebackend.vip.model.vo.VipOrderVO;
import com.example.picturebackend.vip.model.vo.VipPlanVO;
import com.example.picturebackend.vip.model.vo.VipStatusVO;

import java.util.List;

public interface VipService {

    List<VipPlanVO> listPlans();

    VipStatusVO getStatus(Long userId);

    VipOrderVO createOrder(VipOrderCreateRequest request, Long userId);

    VipOrderVO mockPay(String orderNo, Long userId);

    VipOrderVO cancelOrder(String orderNo, Long userId);

    IPage<VipOrderVO> pageMyOrders(Long userId, PageRequest pageRequest);
}
