package com.ww.app.admin.config;

import com.mzt.logapi.service.ILogRecordService;
import com.mzt.logapi.starter.annotation.EnableLogRecord;
import com.ww.app.admin.component.MongoOperateLogComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author ww
 * @create 2024-09-19- 11:41
 * @description:
 */
@Slf4j
@Configuration
// 用不上 tenant 这玩意给个空好啦
@EnableLogRecord(tenant = "")
public class OperateLogConfiguration {

    @Bean
    @Primary
    public ILogRecordService logRecordService() {
        log.info("init mongodb logRecord component...");
        return new MongoOperateLogComponent();
    }

}
