package com.example.picturebackend.interceptor;

import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix:Bearer }")
    private String prefix;

    public AuthInterceptor(JwtUtils jwtUtils, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader(header);
        if (auth == null || !auth.startsWith(prefix)) {
            writeUnauthorized(response, "缺少有效 token");
            return false;
        }
        String token = auth.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            writeUnauthorized(response, "token 为空");
            return false;
        }

        Boolean blacklisted = redisTemplate.hasKey(UserConstant.JWT_BLACKLIST_KEY_PREFIX + token);
        if (Boolean.TRUE.equals(blacklisted)) {
            writeUnauthorized(response, "token 已失效，请重新登录");
            return false;
        }

        Long userId;
        String role;
        try {
            userId = jwtUtils.getUserId(token);
            role = jwtUtils.getRole(token);
        } catch (Exception e) {
            writeUnauthorized(response, e.getMessage());
            return false;
        }

        String currentToken = redisTemplate.opsForValue().get(UserConstant.LOGIN_USER_KEY_PREFIX + userId);
        if (currentToken == null || !currentToken.equals(token)) {
            writeUnauthorized(response, "登录状态已失效");
            return false;
        }

        request.setAttribute(UserConstant.CURRENT_USER_ID_ATTR, userId);
        request.setAttribute(UserConstant.CURRENT_USER_ROLE_ATTR, role);
        request.setAttribute(UserConstant.CURRENT_USER_TOKEN_ATTR, token);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ResultUtils.error(ErrorCode.NOT_LOGIN, message)));
    }
}