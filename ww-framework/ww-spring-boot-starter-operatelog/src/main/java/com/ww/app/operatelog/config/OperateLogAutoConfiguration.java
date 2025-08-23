package com.ww.app.operatelog.config;

import com.mzt.logapi.service.ILogRecordService;
import com.mzt.logapi.starter.annotation.EnableLogRecord;
import com.ww.app.operatelog.core.MongoOperateLogServiceImpl;
import com.ww.app.operatelog.core.service.OperateLogService;
import com.ww.app.operatelog.core.service.impl.OperateLogServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * @author ww
 * @create 2024-09-19- 11:41
 * @description:
 */
@Slf4j
@AutoConfiguration
// 用不上 tenant 这玩意给个空好啦
@EnableLogRecord(tenant = "")
@EnableConfigurationProperties(MongoProperties.class)
public class OperateLogAutoConfiguration {

    @Bean
    @Primary
    public ILogRecordService logRecordService() {
        log.info("init mongodb logRecord component...");
        return new MongoOperateLogServiceImpl();
    }

    @Bean
    public OperateLogService operateLogService() {
        return new OperateLogServiceImpl();
    }

}
