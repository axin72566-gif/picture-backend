package com.example.picturebackend.coupon.constant;

public final class CouponConstant {

    public static final int ACTIVITY_STATUS_OFF = 0;

    public static final int ACTIVITY_STATUS_ON = 1;

    public static final String REDIS_STOCK_KEY_PREFIX = "coupon:seckill:";

    public static final String REDIS_STOCK_KEY_SUFFIX = ":stock";

    public static final String REDIS_USERS_KEY_SUFFIX = ":users";

    /** Lua: already claimed */
    public static final long CLAIM_RESULT_ALREADY = -1L;

    /** Lua: sold out or stock missing */
    public static final long CLAIM_RESULT_SOLD_OUT = -2L;

    private CouponConstant() {
    }

    public static String stockKey(Long activityId) {
        return REDIS_STOCK_KEY_PREFIX + activityId + REDIS_STOCK_KEY_SUFFIX;
    }

    public static String usersKey(Long activityId) {
        return REDIS_STOCK_KEY_PREFIX + activityId + REDIS_USERS_KEY_SUFFIX;
    }
}
