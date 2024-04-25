package com.ww.mall.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MallMinioProperties {

    private String endpoint;

    private String accessKey;

    private String secretKey;

}
