package com.example.picturebackend.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 可选鉴权：有合法 token 时写入当前用户，无 token / 无效时放行（不强制登录）。
 * 用于 follow/status 等「未登录返回默认值、登录后需识别身份」的公开接口。
 */
@Component
public class OptionalAuthInterceptor implements HandlerInterceptor {

    private final AuthTokenResolver authTokenResolver;

    public OptionalAuthInterceptor(AuthTokenResolver authTokenResolver) {
        this.authTokenResolver = authTokenResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        AuthTokenResolver.AuthContext context = authTokenResolver.resolve(request);
        if (context != null) {
            authTokenResolver.apply(request, context);
        }
        return true;
    }
}
