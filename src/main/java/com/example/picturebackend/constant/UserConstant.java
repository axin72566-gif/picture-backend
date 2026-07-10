package com.example.picturebackend.constant;

public final class UserConstant {

    private UserConstant() {
    }

    public static final String ROLE_USER = "user";

    public static final String ROLE_ADMIN = "admin";

    public static final String DEFAULT_USER_PREFIX = "user_";

    public static final String LOGIN_USER_KEY_PREFIX = "user:login:";

    public static final String JWT_BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    public static final String CURRENT_USER_ID_ATTR = "currentUserId";

    public static final String CURRENT_USER_ROLE_ATTR = "currentUserRole";

    public static final String CURRENT_USER_TOKEN_ATTR = "currentUserToken";
}