package com.ww.app.member.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 签到日期校验工具类
 */
@Slf4j
@Component
public class SignDateValidator {

    /**
     * 允许补签的最大天数（过去30天内可补签）
     */
    private static final int MAX_RESIGN_DAYS = 30;

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
     * 校验补签日期是否合法
     *
     * @param dateStr 日期字符串，格式：yyyy-MM-dd
     * @return 是否合法
     */
    public boolean isValidResignDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            // 补签必须指定日期
            return false;
        }

        try {
            // 检查日期格式是否正确
            LocalDate date = LocalDate.parse(dateStr, DatePattern.NORM_DATE_FORMATTER);
            LocalDate today = LocalDate.now();
            
            // 不允许未来日期补签
            if (date.isAfter(today)) {
                return false;
            }
            
            // 不允许当天补签
            if (date.isEqual(today)) {
                return false;
            }
            
            // 检查是否在允许补签的时间范围内
            long daysBetween = ChronoUnit.DAYS.between(date, today);
            return daysBetween <= MAX_RESIGN_DAYS;
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