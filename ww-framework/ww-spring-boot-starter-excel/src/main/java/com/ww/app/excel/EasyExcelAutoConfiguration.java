package com.ww.app.excel;

import lombok.extern.slf4j.Slf4j;
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
    public ExcelMinioTemplate excelMinioTemplate() {
        log.info("初始化excelMinioTemplate功能成功...");
        return new ExcelMinioTemplate();
    }

}
