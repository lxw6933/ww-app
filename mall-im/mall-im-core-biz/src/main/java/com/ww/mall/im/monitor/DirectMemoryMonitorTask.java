package com.ww.mall.im.monitor;

import cn.hutool.core.util.ReflectUtil;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.exception.ApiException;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2024-05-07- 09:30
 * @description: 堆外内存监控
 */
@Slf4j
@Configuration
@EnableScheduling
public class DirectMemoryMonitorTask {

    private static final String MONITOR_KEY = "netty-direct-memory";
    private AtomicLong directMemory;

    @PostConstruct
    public void init() {
        Field field = ReflectUtil.getField(PlatformDependent.class, "DIRECT_MEMORY_COUNTER");
        field.setAccessible(true);
        try {
            directMemory = (AtomicLong) field.get(PlatformDependent.class);
        } catch (IllegalAccessException e) {
            log.error("获取redirect memory异常: {}", e.getMessage());
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        }
    }

    /**
     * 每隔5s统计一下堆外直接内存
     */
    @Scheduled(fixedRate = 10000)
    public void report() {
        int currentMemory = (int) (directMemory.get() / 1024);
        log.info("[{}]:【{}k】", MONITOR_KEY, currentMemory);
    }

}
