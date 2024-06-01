package com.ww.mall.excel;

import com.ww.mall.excel.aspect.ExcelHandlerAspect;
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
public class MallEasyExcelConfiguration {

    @Bean
    public ExcelManager excelManager() {
        log.info("初始化excelManager功能成功...");
        return new ExcelManager();
    }

    @Bean
    public ExcelHandlerAspect excelHandlerAspect() {
        return new ExcelHandlerAspect();
    }

}
