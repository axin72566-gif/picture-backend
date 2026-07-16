package com.example.picturebackend.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN(40100, "未登录"),
    NO_AUTH(40101, "无权限"),
    TOO_MANY_REQUEST(42900, "请求过于频繁"),
    SERVER_ERROR(50000, "服务器内部错误"),

    USER_EXIST(41001, "账号已存在"),
    USER_NOT_FOUND(41002, "用户不存在"),
    PASSWORD_ERROR(41003, "密码错误"),
    ACCOUNT_FORBIDDEN(41004, "账号已被禁用"),

    VIP_PLAN_NOT_FOUND(42001, "套餐不存在"),
    VIP_ORDER_NOT_FOUND(42002, "订单不存在"),
    VIP_ORDER_STATUS_ERROR(42003, "订单状态不允许此操作"),
    VIP_ORDER_EXPIRED(42004, "订单已过期"),
    VIP_PENDING_ORDER_EXISTS(42005, "已有待支付订单"),
    VIP_SPACE_QUOTA_EXCEEDED(42006, "团队空间额度已满"),
    VIP_MEMBER_QUOTA_EXCEEDED(42007, "空间成员额度已满"),

    COUPON_ACTIVITY_NOT_FOUND(42101, "优惠券活动不存在"),
    COUPON_ACTIVITY_NOT_STARTED(42102, "优惠券活动未开始"),
    COUPON_ACTIVITY_ENDED(42103, "优惠券活动已结束"),
    COUPON_SOLD_OUT(42104, "优惠券已抢完"),
    COUPON_ALREADY_CLAIMED(42105, "已领取过该活动优惠券"),
    COUPON_NOT_FOUND(42106, "优惠券不存在"),
    COUPON_STATUS_ERROR(42107, "优惠券状态不允许此操作"),
    COUPON_EXPIRED(42108, "优惠券已过期");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}