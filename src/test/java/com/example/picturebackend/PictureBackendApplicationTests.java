package com.example.picturebackend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文加载测试需要 MySQL 与 Redis 实际可达 (application.yml 中配置的 localhost:3306 / localhost:6379)。
 * 启动 docker compose up -d mysql redis 后,移除 @Disabled 即可手动启用。
 * 日常 CI 只需要跑 UserServiceImplTest 等 Mockito 单测,因此默认禁用。
 */
@SpringBootTest
@Disabled("需先 docker compose up -d mysql redis")
class PictureBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}