package com.example.picturebackend.vip.constant;

/**
 * VIP 常量：支付窗口与空间额度。
 */
public final class VipConstant {

    /** 下单后支付有效分钟数 */
    public static final int PAY_TIMEOUT_MINUTES = 15;

    /** 免费用户可创建（作为 CREATOR）的空间数 */
    public static final int FREE_MAX_OWNED_SPACES = 1;

    /** VIP 可创建的空间数 */
    public static final int VIP_MAX_OWNED_SPACES = 5;

    /** 免费用户单空间成员上限（含创建者） */
    public static final int FREE_MAX_MEMBERS_PER_SPACE = 5;

    /** VIP 单空间成员上限（含创建者） */
    public static final int VIP_MAX_MEMBERS_PER_SPACE = 50;

    /** 套餐上架 */
    public static final int PLAN_STATUS_ON = 1;

    private VipConstant() {
    }
}
