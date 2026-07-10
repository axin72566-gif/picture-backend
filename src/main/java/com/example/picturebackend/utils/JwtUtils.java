package com.example.picturebackend.utils;

import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expireMillis;

    private SecretKey signingKey;

    private SecretKey getKey() {
        if (signingKey == null) {
            signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return signingKey;
    }

    public String generate(Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NOT_LOGIN, "token 无效或已过期");
        }
    }

    public Long getUserId(String token) {
        Object userId = parse(token).get("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "token 缺少用户信息");
        }
        return Long.valueOf(userId.toString());
    }

    public String getRole(String token) {
        Object role = parse(token).get("role");
        return role == null ? null : role.toString();
    }

    public long getExpireMillis() {
        return expireMillis;
    }
}