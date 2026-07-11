package com.example.picturebackend.interceptor;

import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.ResultUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthTokenResolver authTokenResolver;

    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthTokenResolver authTokenResolver, ObjectMapper objectMapper) {
        this.authTokenResolver = authTokenResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        AuthTokenResolver.AuthContext context = authTokenResolver.resolve(request);
        if (context == null) {
            writeUnauthorized(response, "未登录或登录已失效");
            return false;
        }

        authTokenResolver.apply(request, context);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ResultUtils.error(ErrorCode.NOT_LOGIN, message)));
    }
}
