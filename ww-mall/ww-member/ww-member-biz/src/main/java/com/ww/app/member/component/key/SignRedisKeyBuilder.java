package com.ww.app.member.component.key;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * @author ww
 * @create 2025-06-20- 11:09
 * @description:
 */
@Component
public class SignRedisKeyBuilder extends RedisKeyBuilder {

    public static final String SIGN_KEY = "sign";

    public static final String WEEK_FLAG = "W";

    public static final String RESIGN_COUNT_KEY = "resign:count";

    public String buildMonthlySignPrefixKey(Long userId, LocalDate date) {
        // 构建月签到的key，格式为：前缀:用户ID:年月
        String dateStr = date.format(DateTimeFormatter.ofPattern(DatePattern.SIMPLE_MONTH_PATTERN));
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, SIGN_KEY, userId, dateStr);
    }

    public String buildWeeklySignPrefixKey(Long userId, LocalDate date) {
        // 构建周签到的key，格式为：前缀:用户ID:年W周数
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int year = date.get(weekFields.weekBasedYear());
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, SIGN_KEY, userId, year + WEEK_FLAG + weekNumber);
    }

    public static void main(String[] args) {
        SignRedisKeyBuilder signRedisKeyBuilder = new SignRedisKeyBuilder();
        System.out.println(signRedisKeyBuilder.buildWeeklySignPrefixKey(1L, LocalDate.now()));
    }

    public String buildResignCountPrefixKey(Long userId, LocalDate date) {
        // 构建补签的key，格式为：前缀:用户ID:年月
        String dateStr = date.format(DateTimeFormatter.ofPattern(DatePattern.SIMPLE_MONTH_PATTERN));
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RESIGN_COUNT_KEY, userId, dateStr);
    }

}
