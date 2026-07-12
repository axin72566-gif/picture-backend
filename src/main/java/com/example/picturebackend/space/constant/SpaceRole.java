package com.example.picturebackend.space.constant;

/**
 * 团队空间成员角色。
 */
public final class SpaceRole {

    public static final String CREATOR = "CREATOR";

    public static final String EDITOR = "EDITOR";

    public static final String VIEWER = "VIEWER";

    private SpaceRole() {
    }

    public static boolean isAssignable(String role) {
        return EDITOR.equals(role) || VIEWER.equals(role);
    }

    public static boolean isValid(String role) {
        return CREATOR.equals(role) || isAssignable(role);
    }

    /**
     * 角色等级：VIEWER &lt; EDITOR &lt; CREATOR。
     */
    public static int level(String role) {
        if (CREATOR.equals(role)) {
            return 3;
        }
        if (EDITOR.equals(role)) {
            return 2;
        }
        if (VIEWER.equals(role)) {
            return 1;
        }
        return 0;
    }

    /**
     * 当前角色是否不低于所需角色。
     */
    public static boolean atLeast(String role, String minRole) {
        return level(role) >= level(minRole);
    }
}
