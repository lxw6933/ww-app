package com.ww.mall.common.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * @description: cron表达式工具类
 * @author: ww
 * @create: 2021-05-12 19:57
 */
public class CronExpressionUtils {

    private CronExpressionUtils() {
    }

    /**
     * 时间转换成corn表达式
     * 如：2019-04-28 00:30:30，表示定时任务会在2019-04-28 00:30:30执行
     *
     * @param date 执行日期
     * @return String
     */
    public static String conversion(Date date) {
        StringBuilder str = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        str.append(calendar.get(Calendar.SECOND))
                .append(" ")
                .append(calendar.get(Calendar.MINUTE))
                .append(" ")
                .append(calendar.get(Calendar.HOUR_OF_DAY))
                .append(" ")
                .append(calendar.get(Calendar.DAY_OF_MONTH))
                .append(" ")
                .append(calendar.get(Calendar.MONTH) + 1)
                .append(" ")
                .append("?")
                .append(" ")
                .append(calendar.get(Calendar.YEAR));
        return str.toString();
    }

}
