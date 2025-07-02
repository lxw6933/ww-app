package com.ww.app.redis.component.pvuv.keys;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PV UV Redis键构建器
 * 继承自RedisKeyBuilder，提供PV UV相关的键构建方法
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
     * 构建PV键
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return 完整的PV键
     */
    public String buildPvKey(String key, LocalDate date) {
        String dateStr = date != null ? date.format(DATE_FORMATTER) : StrUtil.EMPTY;
        return super.getPrefix() + StringUtils.joinWith(SPLIT_ITEM, PV_KEY, key, dateStr);
    }

    /**
     * 构建UV键
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return 完整的UV键
     */
    public String buildUvKey(String key, LocalDate date) {
        String dateStr = date != null ? date.format(DATE_FORMATTER) : StrUtil.EMPTY;
        return super.getPrefix() + StringUtils.joinWith(SPLIT_ITEM, UV_KEY, key, dateStr);
    }

} 