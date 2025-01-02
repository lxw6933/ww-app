package com.ww.app.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class XxlJobAutoConfiguration {

    @Value("${xxl.job.admin.address}")
    private String adminAddress;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Value("${xxl.job.executor.appName:'xxl-job-executor-sample'}")
    private String executorAppName;

    @Value("${xxl.job.executor.address:}")
    private String executorAddress;

    @Value("${xxl.job.executor.ip:}")
    private String executorIp;

    @Value("${xxl.job.executor.port:9999}")
    private int executorPort;

    @Value("${xxl.job.executor.logPath:'/data/applogs/xxl-job/jobhandler'}")
    private String executorLogPath;

    @Value("${xxl.job.executor.logRetentionDays:30}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("xxl-job config init");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddress);
        xxlJobSpringExecutor.setAppname(executorAppName);
        if (StringUtils.isNotEmpty(accessToken)) {
            xxlJobSpringExecutor.setAccessToken(accessToken);
        }
        if (StringUtils.isNotEmpty(executorAddress)) {
            xxlJobSpringExecutor.setAddress(executorAddress);
        }

        if (StringUtils.isNotEmpty(executorIp)) {
            xxlJobSpringExecutor.setIp(executorIp);
        }
        xxlJobSpringExecutor.setPort(executorPort);
        xxlJobSpringExecutor.setLogPath(executorLogPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        return xxlJobSpringExecutor;
    }

}
