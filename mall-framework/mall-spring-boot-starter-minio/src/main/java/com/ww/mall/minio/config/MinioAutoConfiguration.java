package com.ww.mall.minio.config;

import com.ww.mall.minio.MallMinioS3Client;
import com.ww.mall.minio.MallMinioTemplate;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({MinioClient.class, MongoTemplate.class})
@EnableConfigurationProperties({MallMinioProperties.class})
public class MinioAutoConfiguration {

    @Bean
    public MallMinioS3Client mallMinioS3Client(MallMinioProperties mallMinioProperties) {
        MinioAsyncClient minioAsyncClient = MinioAsyncClient.builder()
                .endpoint(mallMinioProperties.getEndpoint())
                .credentials(mallMinioProperties.getAccessKey(), mallMinioProperties.getSecretKey())
                .build();
        return new MallMinioS3Client(minioAsyncClient);
    }

    @Bean
    public MinioClient minioClient(MallMinioProperties mallMinioProperties) {
        return MinioClient.builder()
                .endpoint(mallMinioProperties.getEndpoint())
                .credentials(mallMinioProperties.getAccessKey(), mallMinioProperties.getSecretKey())
                .build();
    }

    @Bean
    public MallMinioTemplate mallMinioTemplate(MinioClient minioClient, MallMinioS3Client mallMinioS3Client) {
        return new MallMinioTemplate(minioClient, mallMinioS3Client);
    }

}
