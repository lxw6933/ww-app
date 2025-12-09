package com.ww.mall.promotion.config.cache;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.service.group.GroupActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 促销活动本地缓存配置
 */
@Slf4j
@Configuration
public class PromotionCacheConfiguration {

    @Resource
    private GroupActivityService groupActivityService;

    @Bean
    public LoadingCache<String, GroupActivity> groupActivityCache() {
        return CaffeineUtil.createAutoRefreshCache(
                200,
                1000,
                1,
                TimeUnit.HOURS,
                30,
                TimeUnit.MINUTES,
                activityId -> {
                    log.info("group-activity[{}]本地缓存未命中，查询数据库", activityId);
                    return groupActivityService.getActivityById(activityId);
                }
        );
    }
}

