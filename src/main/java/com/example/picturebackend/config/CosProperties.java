package com.example.picturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cos")
public class CosProperties {

    private String secretId;

    private String secretKey;

    private String region;

    private String bucket;

    private String baseUrl;
}
