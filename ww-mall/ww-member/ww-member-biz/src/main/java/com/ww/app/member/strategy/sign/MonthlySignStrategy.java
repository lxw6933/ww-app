package com.ww.app.member.strategy.sign;

import cn.hutool.core.date.DatePattern;
import com.ww.app.member.enums.SignPeriodEnum;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * @author ww
 * @create 2023-07-21- 09:18
 * @description: 月签到策略实现
 */
@Component
public class MonthlySignStrategy extends AbstractSignStrategy {

    @Override
    protected void processSignReward(Long userId, LocalDate date) {
        // 获取签到次数
        String signKey = buildSignKey(userId, date);
        Long signCount = stringRedisTemplate.execute(
                (RedisCallback<Long>) con -> con.bitCount(signKey.getBytes())
        );
        // 半月签到奖励
        if (signCount != null && signCount == 15) {
            // TODO: 实现半月签到奖励逻辑
        }
        // 满月签到奖励
        int daysInMonth = getBitCount(date);
        if (signCount != null && signCount.intValue() == daysInMonth) {
            // TODO: 实现满月签到奖励逻辑
        }
    }

    @Override
    public int getOffset(LocalDate date) {
        // 获取当天是月份中的第几天，偏移量从0开始
        return date.getDayOfMonth() - 1;
    }

    @Override
    public int getBitCount(LocalDate date) {
        // 获取月份的总天数
        return YearMonth.from(date).lengthOfMonth();
    }

    @Override
    protected String buildSignKey(Long userId, LocalDate date) {
        return signRedisKeyBuilder.buildMonthlySignPrefixKey(userId, date);
    }

    @Override
    protected void fillSignInfo(LocalDate date, long bitValue, Map<String, Boolean> signInfo) {
        // 获取月份的总天数
        int daysInMonth = getBitCount(date);
        // 从低位到高位进行遍历，为 0 表示未签到，为 1 表示已签到
        for (int i = daysInMonth; i > 0; i--) {
            // 构建日期
            LocalDate currentDate = date.withDayOfMonth(i);
            // 先右移再左移，如果不等于自己说明签到，否则未签到
            boolean flag = bitValue >> 1 << 1 != bitValue;
            // 存放当月每天的签到情况
            signInfo.put(currentDate.format(DatePattern.NORM_DATE_FORMATTER), flag);
            
            bitValue >>= 1;
        }
    }

    @Override
    public SignPeriodEnum getType() {
        return SignPeriodEnum.MONTHLY;
    }

    @Override
    public int getResignConfig() {
        return 3;
    }
} 