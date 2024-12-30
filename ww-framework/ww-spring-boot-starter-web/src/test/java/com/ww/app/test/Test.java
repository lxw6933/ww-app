package com.ww.app.test;

import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.utils.CaffeineUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ww
 * @create 2024-10-29- 17:55
 * @description:
 */
public class Test {

    public static void main(String[] args) {
//        System.out.println(DateUtil.between(new Date(), DateUtil.parseDateTime("2024-10-29"), DateUnit.HOUR, false));
//        System.out.println(DateUtil.between(new Date(), DateUtil.parseDateTime("2024-10-29 18:16:00"), DateUnit.MS, false));
//        System.out.println(DateUtil.parseDate("2024-10-29"));
//        System.out.println(new BigDecimal("4799.99").divide(BigDecimal.valueOf(12), 2, RoundingMode.DOWN));
//        String str = "ds";
//        System.out.println(StrUtil.hide(str, 0, str.length() - 3));
        Cache<Object, Object> cache = CaffeineUtil.initCaffeine();
//        Object key = new Object();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                A key = new A("1321321");
                if (cache.asMap().putIfAbsent(key, Boolean.TRUE) == null) {
                    System.out.println("==============");
                }
            });
        }
        executor.shutdown();
    }

    @Data
    @AllArgsConstructor
    static class A {
        private String name;
    }

}
