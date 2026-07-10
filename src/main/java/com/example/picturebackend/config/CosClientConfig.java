package com.example.picturebackend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosClientConfig {

    @Bean
    public COSClient cosClient(CosProperties cosProperties) {
        COSCredentials cred = new BasicCOSCredentials(
                cosProperties.getSecretId(), cosProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        return new COSClient(cred, clientConfig);
    }
}
