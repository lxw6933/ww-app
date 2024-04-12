package com.ww.mall.web.utils;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-07-15- 14:48
 * @description:
 */
@Slf4j
public class IdUtil {

    private static long workerId;

    private static long dataCenterId;

    static {
        try {
            workerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr()) % 32;
            dataCenterId = RandomUtils.nextInt(0, 31);
            log.info("当前服务器的workerId：{} dataCenterId：{}", workerId, dataCenterId);
        } catch (Exception e) {
            log.error("获取服务器机器id失败：{}", e.getMessage());
            workerId = RandomUtils.nextInt(0, 31);
            dataCenterId = RandomUtils.nextInt(0, 31);
            log.info("异常后分配的服务器的workerId：{} dataCenterId：{}", workerId, dataCenterId);
        }
    }

    public static long generatorId() {
        Snowflake snowflake = Singleton.get(Snowflake.class, workerId, dataCenterId, true);
        return snowflake.nextId();
    }

    public static String generatorIdStr() {
        Snowflake snowflake = Singleton.get(Snowflake.class, workerId, dataCenterId, true);
        return snowflake.nextIdStr();
    }

    public static void generatorMoreId() throws InterruptedException {
        int size = 100000;
        Set<String> container = new ConcurrentHashSet<>(size);
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        for (int i = 0; i < size; i++) {
            executorService.submit(() -> container.add(generatorIdStr()));
        }
        waitFinish(executorService);
        log.info("数量：" + container.size());
    }

    public static void main(String[] args) throws InterruptedException {
        IdUtil.generatorMoreId();
    }

    private static void waitFinish(ExecutorService executorService) throws InterruptedException {
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                // 立即终止线程池
                executorService.shutdownNow();
                throw e;
            }
        }
    }


}
