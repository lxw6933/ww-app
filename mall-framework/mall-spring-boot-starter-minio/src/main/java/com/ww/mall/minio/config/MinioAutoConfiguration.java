package com.ww.mall.minio.config;

import com.ww.mall.minio.MinioS3Client;
import com.ww.mall.minio.MinioTemplate;
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
@EnableConfigurationProperties({MinioProperties.class})
public class MinioAutoConfiguration {

    @Bean
    public MinioS3Client mallMinioS3Client(MinioProperties minioProperties) {
        MinioAsyncClient minioAsyncClient = MinioAsyncClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        return new MinioS3Client(minioAsyncClient);
    }

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean
    public MinioTemplate mallMinioTemplate(MinioClient minioClient, MinioS3Client minioS3Client) {
        return new MinioTemplate(minioClient, minioS3Client);
    }

}
