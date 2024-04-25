package com.ww.mall.minio;

import com.ww.mall.minio.java.MallMinioUtil;
import com.ww.mall.minio.s3.MallMinioS3Client;
import com.ww.mall.minio.s3.MallMinioS3Util;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MallMinioProperties.class)
public class MinioAutoConfiguration {

    @Bean
    public MinioAsyncClient minioAsyncClient(MallMinioProperties mallMinioProperties) {
        return MinioAsyncClient.builder()
                .endpoint(mallMinioProperties.getEndpoint())
                .credentials(mallMinioProperties.getAccessKey(), mallMinioProperties.getSecretKey())
                .build();
    }

    @Bean
    public MinioClient minioClient(MallMinioProperties mallMinioProperties) {
        return MinioClient.builder()
                .endpoint(mallMinioProperties.getEndpoint())
                .credentials(mallMinioProperties.getAccessKey(), mallMinioProperties.getSecretKey())
                .build();
    }

    @Bean
    public MallMinioUtil mallMinioUtil(MinioClient minioClient) {
        return new MallMinioUtil(minioClient);
    }

    @Bean
    public MallMinioS3Util mallMinioS3Util(MinioAsyncClient minioAsyncClient) {
        return new MallMinioS3Util(new MallMinioS3Client(minioAsyncClient));
    }

}
