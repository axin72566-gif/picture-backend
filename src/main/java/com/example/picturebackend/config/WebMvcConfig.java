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
        // 可选登录：有合法 token 时识别当前用户（关注态 / 点赞态 / 列表 liked）
        registry.addInterceptor(optionalAuthInterceptor)
                .addPathPatterns(
                        "/api/user/{id:\\d+}/follow/status",
                        "/api/picture/page",
                        "/api/picture/{id:\\d+}",
                        "/api/picture/{id:\\d+}/likes",
                        "/api/picture/{id:\\d+}/like/status"
                );

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
                        "/api/picture/{id:\\d+}",
                        "/api/picture/{id:\\d+}/likes",
                        "/api/picture/{id:\\d+}/like/status"
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