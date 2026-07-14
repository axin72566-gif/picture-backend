package com.example.picturebackend.admin;

import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

public final class AdminAuth {

    private AdminAuth() {
    }

    public static Long requireAdminUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String role = (String) request.getAttribute(UserConstant.CURRENT_USER_ROLE_ATTR);
        if (!UserConstant.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "需要管理员权限");
        }
        return userId;
    }
}
