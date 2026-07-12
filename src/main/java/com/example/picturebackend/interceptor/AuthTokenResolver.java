package com.example.picturebackend.interceptor;

import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 从请求头或原始 token 解析登录态。校验失败时返回 null，供强制鉴权、可选鉴权与 WebSocket 握手共用。
 */
@Component
public class AuthTokenResolver {

    public record AuthContext(Long userId, String role, String token) {
    }

    private final JwtUtils jwtUtils;

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix:Bearer }")
    private String prefix;

    public AuthTokenResolver(JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    public AuthContext resolve(HttpServletRequest request) {
        String auth = request.getHeader(header);
        if (auth == null || !auth.startsWith(prefix)) {
            return null;
        }
        String token = auth.substring(prefix.length()).trim();
        return resolveToken(token);
    }

    /**
     * 校验原始 JWT（如 WebSocket query 参数），失败返回 null。
     */
    public AuthContext resolveToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        token = token.trim();

        Boolean blacklisted = redisTemplate.hasKey(UserConstant.JWT_BLACKLIST_KEY_PREFIX + token);
        if (Boolean.TRUE.equals(blacklisted)) {
            return null;
        }

        Long userId;
        String role;
        try {
            userId = jwtUtils.getUserId(token);
            role = jwtUtils.getRole(token);
        } catch (Exception e) {
            return null;
        }

        String currentToken = redisTemplate.opsForValue().get(UserConstant.LOGIN_USER_KEY_PREFIX + userId);
        if (currentToken == null || !currentToken.equals(token)) {
            return null;
        }

        return new AuthContext(userId, role, token);
    }

    public void apply(HttpServletRequest request, AuthContext context) {
        request.setAttribute(UserConstant.CURRENT_USER_ID_ATTR, context.userId());
        request.setAttribute(UserConstant.CURRENT_USER_ROLE_ATTR, context.role());
        request.setAttribute(UserConstant.CURRENT_USER_TOKEN_ATTR, context.token());
    }
}
