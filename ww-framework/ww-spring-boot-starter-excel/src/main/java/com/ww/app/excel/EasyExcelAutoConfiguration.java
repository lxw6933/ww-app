package com.ww.app.excel;

import com.ww.app.excel.aspect.ExcelHandlerAspect;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-06-01 10:46
 * @description:
 */
@Slf4j
@Configuration
public class EasyExcelAutoConfiguration {

    @Bean
    public ExcelTemplate excelTemplate() {
        log.info("初始化excelTemplate功能成功...");
        return new ExcelTemplate();
    }

    @Bean
    @ConditionalOnClass(MinioClient.class)
    public ExcelMinioTemplate excelMinioTemplate() {
        log.info("初始化excelMinioTemplate功能成功...");
        return new ExcelMinioTemplate();
    }

    @Bean
    public ExcelHandlerAspect excelHandlerAspect() {
        return new ExcelHandlerAspect();
    }

}
