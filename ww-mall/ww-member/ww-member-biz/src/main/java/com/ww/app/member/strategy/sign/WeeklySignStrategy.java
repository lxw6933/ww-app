package com.ww.app.member.strategy.sign;

import com.ww.app.member.enums.SignPeriod;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;

import static com.ww.app.member.component.key.SignRedisKeyBuilder.WEEK_FLAG;

/**
 * @author ww
 * @create 2023-07-21- 09:18
 * @description: 周签到策略实现
 */
@Component
public class WeeklySignStrategy extends AbstractSignStrategy {

    @Override
    protected void processSignReward(Long userId, LocalDate date) {
        // 获取签到次数
        String signKey = buildSignKey(userId, date);
        Long signCount = stringRedisTemplate.execute(
                (RedisCallback<Long>) con -> con.bitCount(signKey.getBytes())
        );
        // 获取当天是周几
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        // 如果是周末且已签到5天以上，发放额外奖励
        if ((dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
                && signCount != null && signCount >= 5) {
            // TODO: 实现周末额外奖励逻辑
        }
    }

    @Override
    public int getOffset(LocalDate date) {
        // 获取当天是周几，从0（周一）到6（周日）
        return date.getDayOfWeek().getValue() - 1;
    }

    @Override
    public int getBitCount(LocalDate date) {
        // 周签到使用7位，对应周一到周日
        return 7;
    }

    @Override
    public LocalDate getEndDate(String periodKey) {
        // 解析年份和周数
        String[] parts = periodKey.split(WEEK_FLAG);
        if (parts.length != 2) {
            throw new IllegalArgumentException("周数格式错误，应为: yyyyWww");
        }

        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);

        // 获取该周的第一天（周一）
        LocalDate firstDayOfWeek = LocalDate.of(year, 1, 1)
                .with(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(), week)
                .with(DayOfWeek.MONDAY);

        // 获取该周的最后一天（周日）
        return firstDayOfWeek.with(DayOfWeek.SUNDAY);
    }

    @Override
    protected String buildSignKey(Long userId, LocalDate date) {
        return signRedisKeyBuilder.buildWeeklySignPrefixKey(userId, date);
    }

    protected void fillSignInfo(LocalDate date, long bitValue, Map<LocalDate, Boolean> signInfo) {
        // 获取本周的第一天（周一）
        LocalDate firstDayOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // 从低位到高位进行遍历，为 0 表示未签到，为 1 表示已签到
        for (int i = 7; i > 0; i--) {
            // 计算当前日期
            LocalDate currentDate = firstDayOfWeek.plusDays(i - 1);
            // 先右移再左移，如果不等于自己说明签到，否则未签到
            boolean flag = bitValue >> 1 << 1 != bitValue;
            // 存放当周每天的签到情况
            signInfo.put(currentDate, flag);

            bitValue >>= 1;
        }
    }

    @Override
    public SignPeriod getType() {
        return SignPeriod.WEEKLY;
    }

    @Override
    public int getResignConfig() {
        return 0;
    }
} 