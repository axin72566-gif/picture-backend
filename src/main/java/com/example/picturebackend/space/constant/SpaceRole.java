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
}
