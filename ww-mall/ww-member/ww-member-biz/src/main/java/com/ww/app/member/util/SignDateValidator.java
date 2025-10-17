package com.ww.app.member.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.enums.SignPeriodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 签到日期校验工具类
 */
@Slf4j
@Component
public class SignDateValidator {

    /**
     * 校验签到日期是否合法
     *
     * @param dateStr 日期字符串，格式：yyyy-MM-dd
     * @return 是否合法
     */
    public boolean isValidSignDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            // 日期为空时默认为当天，合法
            return true;
        }
        try {
            // 检查日期格式是否正确
            LocalDate date = LocalDate.parse(dateStr, DatePattern.NORM_DATE_FORMATTER);
            // 不允许未来日期签到
            LocalDate today = LocalDate.now();
            return !date.isAfter(today);
        } catch (DateTimeParseException e) {
            log.error("日期格式错误: {}", dateStr, e);
            return false;
        }
    }

    /**
     * 周期化补签校验：仅允许在当前周期内的过去日期补签
     * - MONTH: 必须在本月内，且小于今天
     * - WEEK:  必须在本周内（周一为一周开始），且小于今天
     */
    public boolean isValidResignDate(String dateStr, SignPeriodEnum periodType) {
        if (StrUtil.isBlank(dateStr)) {
            return false;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr, DatePattern.NORM_DATE_FORMATTER);
            LocalDate today = LocalDate.now();

            if (!date.isBefore(today)) {
                // 不允许今天及未来
                return false;
            }

            if (periodType == SignPeriodEnum.MONTHLY) {
                return date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
            }

            // WEEK: 以周一为一周开始
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate weekEnd = weekStart.plusDays(6);
            return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
        } catch (DateTimeParseException e) {
            log.error("日期格式错误: {}", dateStr, e);
            return false;
        }
    }

    /**
     * 获取有效的签到日期
     *
     * @param dateStr 日期字符串
     * @return 有效的日期字符串
     */
    public String getValidSignDate(String dateStr) {
        if (isValidSignDate(dateStr)) {
            return StrUtil.isBlank(dateStr) ? 
                    LocalDate.now().format(DatePattern.NORM_DATE_FORMATTER) : dateStr;
        } else {
            throw new ApiException("日期无效");
        }
    }
} 