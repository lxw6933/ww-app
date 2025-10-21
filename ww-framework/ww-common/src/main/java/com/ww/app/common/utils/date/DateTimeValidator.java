package com.ww.app.common.utils.date;

import cn.hutool.core.date.DatePattern;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * @author ww
 * @create 2025-10-21 11:22
 * @description:
 */
public class DateTimeValidator {

    private DateTimeValidator() {}

    public static final String WEEKLY_PATTERN = "yyyy'W'ww";

    // 严格模式的格式化器
    private static final DateTimeFormatter YEAR_WEEK_FORMATTER = DateTimeFormatter
            .ofPattern(WEEKLY_PATTERN, Locale.getDefault())
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter
            .ofPattern(DatePattern.SIMPLE_MONTH_PATTERN, Locale.getDefault())
            .withResolverStyle(ResolverStyle.STRICT);

    /**
     * 校验 yyyy'W'ww 格式
     */
    public static boolean isValidYearWeek(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        try {
            // 尝试解析，使用严格模式会验证周数的有效性
            YEAR_WEEK_FORMATTER.parse(input);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 校验 yyyyMM 格式
     */
    public static boolean isValidYearMonth(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        try {
            // 使用 YearMonth 进行严格验证
            YearMonth.parse(input, YEAR_MONTH_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}
