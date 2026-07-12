package com.example.picturebackend.config;

import com.example.picturebackend.interceptor.AuthInterceptor;
import com.example.picturebackend.interceptor.OptionalAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final OptionalAuthInterceptor optionalAuthInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor, OptionalAuthInterceptor optionalAuthInterceptor) {
        this.authInterceptor = authInterceptor;
        this.optionalAuthInterceptor = optionalAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // follow/status 允许未登录访问，但有 token 时需识别当前用户以返回正确关注态
        registry.addInterceptor(optionalAuthInterceptor)
                .addPathPatterns("/api/user/{id:\\d+}/follow/status");

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login",
                        // 仅放行「数字 id」的公开资料，不能用 /api/user/*（会误伤 /current、/logout、/update 等）
                        "/api/user/{id:\\d+}",
                        "/api/user/{id:\\d+}/followers",
                        "/api/user/{id:\\d+}/following",
                        "/api/user/{id:\\d+}/follow/status",
                        "/api/picture/page",
                        "/api/picture/{id:\\d+}"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}