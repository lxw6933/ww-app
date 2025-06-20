package com.ww.app.member.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * @author ww
 * @create 2025-06-20- 11:09
 * @description:
 */
@Component
public class SignRedisKeyBuilder extends RedisKeyBuilder {

    private static final String MONTHLY_SIGN_KEY = "sign:monthly";
    private static final String WEEKLY_SIGN_KEY = "sign:weekly";

    private static final String RESIGN_COUNT_KEY = "resign:count:";

    public String buildMonthlySignPrefixKey(Long userId, LocalDate date) {
        // 构建月签到的key，格式为：前缀:用户ID:年:月
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, MONTHLY_SIGN_KEY, userId, date.getYear(), date.getMonthValue());
    }

    public String buildWeeklySignPrefixKey(Long userId, LocalDate date) {
        // 构建周签到的key，格式为：前缀:用户ID:年:周数
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int year = date.get(weekFields.weekBasedYear());
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, WEEKLY_SIGN_KEY, userId, year, weekNumber);
    }

    public String buildResignCountPrefixKey(Long userId, LocalDate date) {
        // 构建补签的key，格式为：前缀:用户ID:年:月
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RESIGN_COUNT_KEY, userId, date.getYear(), date.getMonthValue());
    }

}
