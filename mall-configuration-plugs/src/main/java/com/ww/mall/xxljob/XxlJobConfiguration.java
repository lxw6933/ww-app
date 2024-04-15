package com.ww.mall.xxljob;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class XxlJobConfiguration {

    @Value("${xxl.job.admin.address}")
    private String adminAddress;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appName}")
    private String executorAppName;

    @Value("${xxl.job.executor.port}")
    private int executorPort;

    @Value("${xxl.job.executor.logPath}")
    private String executorLogPath;

    @Value("${xxl.job.executor.logRetentionDays}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("xxl-job config init");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddress);
        xxlJobSpringExecutor.setAppname(executorAppName);
        xxlJobSpringExecutor.setPort(executorPort);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(executorLogPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        return xxlJobSpringExecutor;
    }

}
