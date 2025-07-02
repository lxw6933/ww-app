package com.ww.app.redis.component.pvuv.keys;

import cn.hutool.core.date.DatePattern;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PVUV Redis键构建器
 * 继承自RedisKeyBuilder，提供PVUV相关的键构建方法
 */
public class PvUvRedisKeyBuilder extends RedisKeyBuilder {

    /**
     * 日期格式：yyyy-MM-dd
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN);

    /**
     * PV前缀
     */
    private static final String PV_KEY = "pv";

    /**
     * UV前缀
     */
    private static final String UV_KEY = "uv";

    /**
     * 活动前缀
     */
    private static final String EVENT_KEY = "event";

    /**
     * 构建PV键
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return 完整的PV键
     */
    public String buildPvKey(String key, LocalDate date) {
        List<Object> keys = new ArrayList<>();
        keys.add(PV_KEY);
        keys.add(key);
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String dateStr = targetDate.format(DATE_FORMATTER);
        keys.add(dateStr);
        
        return super.getPrefix() + StringUtils.joinWith(SPLIT_ITEM, keys.toArray());
    }

    /**
     * 构建UV键
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return 完整的UV键
     */
    public String buildUvKey(String key, LocalDate date) {
        List<Object> keys = new ArrayList<>();
        keys.add(UV_KEY);
        keys.add(key);
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String dateStr = targetDate.format(DATE_FORMATTER);
        keys.add(dateStr);
        
        return super.getPrefix() + StringUtils.joinWith(SPLIT_ITEM, keys.toArray());
    }

    /**
     * 构建活动键
     *
     * @param eventId 活动ID
     * @return 活动键
     */
    public String buildEventKey(String eventId) {
        List<Object> keys = new ArrayList<>();
        keys.add(EVENT_KEY);
        keys.add(eventId);
        
        return super.getPrefix() + StringUtils.joinWith(SPLIT_ITEM, keys.toArray());
    }
} 