package com.ww.mall.web.config.thread;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 配置不会实时生效到线程池，需重启项目
 * @author: ww
 * @create: 2023/7/16 19:16
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "default.thread")
public class DefaultThreadPoolProperties {

    /**
     * 线程名称
     */
    private String threadName = "ww-mall";

    /**
     * 核心线程数量
     */
    private Integer coreSize = 20;

    /**
     * 最大线程数量
     */
    private Integer maxSize = 100;

    /**
     * 销毁时间（单位：秒）
     */
    private Integer keepAliveTime = 5 * 60;

    /**
     * 阻塞队列大小
     */
    private Integer queueLength = 10000;

}
