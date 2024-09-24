package com.ww.mall.seckill.manager;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.mall.common.utils.MallCaffeineUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-04-11- 11:13
 * @description:
 */
@Slf4j
public class MallCacheManager {

    public static void main(String[] args) {
        System.out.println(TimeUnit.SECONDS.name());
    }

    public static final LoadingCache<String, String> spuCache = MallCaffeineUtil.initAutoSyncRefreshCaffeine(500, 2000, 6, TimeUnit.HOURS, 30, TimeUnit.MINUTES, key -> key + "===");

}
