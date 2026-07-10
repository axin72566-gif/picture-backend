package com.example.picturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan({"com.example.picturebackend.user.mapper", "com.example.picturebackend.picture.mapper"})
public class PictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureBackendApplication.class, args);
    }

}
