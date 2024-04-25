package com.ww.mall.minio;

import com.ww.mall.minio.java.MallMinioUtil;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MallMinioProperties.class)
public class MinioAutoConfiguration {

//    @Bean
//    public MallMinioS3Client mallMinioS3Client(MinioProperties minioProperties) {
//        MinioAsyncClient minioAsyncClient = MinioAsyncClient.builder()
//                .endpoint(minioProperties.getEndpoint())
//                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
//                .build();
//        return new MallMinioS3Client(minioAsyncClient);
//    }

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

}
