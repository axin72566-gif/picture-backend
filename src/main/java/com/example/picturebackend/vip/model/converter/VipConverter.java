package com.example.picturebackend.vip.model.converter;

import com.example.picturebackend.vip.entity.VipOrder;
import com.example.picturebackend.vip.entity.VipPlan;
import com.example.picturebackend.vip.model.vo.VipOrderVO;
import com.example.picturebackend.vip.model.vo.VipPlanVO;

public final class VipConverter {

    private VipConverter() {
    }

    public static VipPlanVO toPlanVO(VipPlan plan) {
        if (plan == null) {
            return null;
        }
        VipPlanVO vo = new VipPlanVO();
        vo.setId(plan.getId());
        vo.setCode(plan.getCode());
        vo.setName(plan.getName());
        vo.setDurationDays(plan.getDurationDays());
        vo.setPriceCents(plan.getPriceCents());
        return vo;
    }

    public static VipOrderVO toOrderVO(VipOrder order, VipPlan plan) {
        if (order == null) {
            return null;
        }
        VipOrderVO vo = new VipOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPlanId(order.getPlanId());
        vo.setDurationDays(order.getDurationDays());
        vo.setOriginalAmountCents(order.getOriginalAmountCents());
        vo.setDiscountCents(order.getDiscountCents());
        vo.setCouponId(order.getCouponId());
        vo.setAmountCents(order.getAmountCents());
        vo.setStatus(order.getStatus());
        vo.setExpireTime(order.getExpireTime());
        vo.setPaidTime(order.getPaidTime());
        vo.setCreateTime(order.getCreateTime());
        if (plan != null) {
            vo.setPlanCode(plan.getCode());
            vo.setPlanName(plan.getName());
        }
        return vo;
    }
}
