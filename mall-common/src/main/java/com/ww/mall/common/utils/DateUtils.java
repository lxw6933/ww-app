package com.ww.mall.common.utils;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @description: 日期工具类
 * @author: ww
 * @create: 2021/5/20 下午9:37
 **/
public class DateUtils {

    public static void main(String[] args) {
        System.out.println(getDate(nextMinute(30),DATETIME_FORMAT));
    }

    private DateUtils() {
    }

    /**
     * 仅显示年月日，例如 2015-08-11.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 显示年月日时分秒，例如 2015-08-11 09:51:53.
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 仅显示时分秒，例如 09:51:53.
     */
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 每天的毫秒数 8640000.
     */
    public static final long MILLISECONDS_PER_DAY = 86400000L;

    /**
     * 每周的天数.
     */
    public static final long DAYS_PER_WEEK = 7L;

    /**
     * 每小时毫秒数.
     */
    public static final long MILLISECONDS_PER_HOUR = 3600000L;

    /**
     * 每分钟秒数.
     */
    public static final long SECONDS_PER_MINUTE = 60L;

    /**
     * 每小时秒数.
     */
    public static final long SECONDS_PER_HOUR = 3600L;

    /**
     * 每天秒数.
     */
    public static final long SECONDS_PER_DAY = 86400L;

    /**
     * 每个月秒数，默认每月30天.
     */
    public static final long SECONDS_PER_MONTH = 2592000L;

    /**
     * 每年秒数，默认每年365天.
     */
    public static final long SECONDS_PER_YEAR = 31536000L;

    /**
     * 常用的时间格式.
     */
    private static final String[] parsePatterns = {"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm"};

    /**
     * 获取当前时间指定格式下的字符串.
     *
     * @param pattern 转化后时间展示的格式，例如"yyyy-MM-dd"，"yyyy-MM-dd HH:mm:ss"等
     * @return String 格式转换之后的时间字符串.
     */
    public static String getDate(String pattern) {
        return getDate(new Date(), pattern);
    }

    /**
     * 获取指定日期的字符串格式.
     *
     * @param date    需要格式化的时间，不能为空
     * @param pattern 时间格式，例如"yyyy-MM-dd"，"yyyy-MM-dd HH:mm:ss"等
     * @return String 格式转换之后的时间字符串.
     */
    public static String getDate(Date date, String pattern) {
        return DateFormatUtils.format(date, pattern);
    }

    /**
     * 获取日期时间字符串，默认格式为（yyyy-MM-dd）.
     *
     * @param date    需要转化的日期时间
     * @param pattern 时间格式，例如"yyyy-MM-dd" "HH:mm:ss" "E"等
     * @return String 格式转换后的时间字符串
     */
    public static String formatDate(Date date, Object... pattern) {
        String formatDate = null;
        if (pattern != null && pattern.length > 0) {
            formatDate = DateFormatUtils.format(date, pattern[0].toString());
        } else {
            formatDate = DateFormatUtils.format(date, DateUtils.DATE_FORMAT);
        }
        return formatDate;
    }

    /**
     * 获取当前年份字符串.
     *
     * @return String 当前年份字符串，例如 2015
     */
    public static String getYear() {
        return formatDate(new Date(), "yyyy");
    }

    /**
     * 获取当前月份字符串.
     *
     * @return String 当前月份字符串，例如 08
     */
    public static String getMonth() {
        return formatDate(new Date(), "MM");
    }

    /**
     * 获取当前天数字符串.
     *
     * @return String 当前天数字符串，例如 11
     */
    public static String getDay() {
        return formatDate(new Date(), "dd");
    }

    /**
     * 获取当前星期字符串.
     *
     * @return String 当前星期字符串，例如星期二
     */
    public static String getWeek() {
        return formatDate(new Date(), "E");
    }

    /**
     * 将日期型字符串转换为日期格式.
     * 支持的日期字符串格式包括"yyyy-MM-dd","yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
     * "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm"
     *
     * @param str str
     * @return Date
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return org.apache.commons.lang3.time.DateUtils.parseDate(str.toString(), parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 获取当前日期与指定日期相隔的天数.
     *
     * @param date 给定的日期
     * @return long 日期间隔天数，正数表示给定日期在当前日期之前，负数表示在当前日期之后
     */
    public static long getDaysNow(Date date) {
        // 将指定日期转换为yyyy-MM-dd格式
        date = DateUtils.parseDate(DateUtils.formatDate(date, DateUtils.DATE_FORMAT));
        // 当前日期转换为yyyy-MM-dd格式
        Date currentDate = DateUtils.parseDate(DateUtils.formatDate(new Date(), DateUtils.DATE_FORMAT));
        long t = 0;
        if (date != null && currentDate != null) {
            t = (currentDate.getTime() - date.getTime()) / DateUtils.MILLISECONDS_PER_DAY;
        }
        return t;
    }

    /**
     * 获取当前日期指定分钟之后的日期.
     *
     * @param num 相隔分钟
     * @return Date 日期
     */
    public static Date nextMinute(int num) {
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.MINUTE, curr.get(Calendar.MINUTE) + num);
        return curr.getTime();
    }

    /**
     * 获取当前日期指定天数之后的日期.
     *
     * @param num 相隔天数
     * @return Date 日期
     */
    public static Date nextDay(int num) {
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.DAY_OF_MONTH, curr.get(Calendar.DAY_OF_MONTH) + num);
        return curr.getTime();
    }

    /**
     * 获取当前日期指定月数之后的日期.
     *
     * @param num 间隔月数
     * @return Date 日期
     */
    public static Date nextMonth(int num) {
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.MONTH, curr.get(Calendar.MONTH) + num);
        return curr.getTime();
    }

    /**
     * 获取当前日期指定年数之后的日期.
     *
     * @param num 间隔年数
     * @return Date 日期
     */
    public static Date nextYear(int num) {
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.YEAR, curr.get(Calendar.YEAR) + num);
        return curr.getTime();
    }

    /**
     * 计算两个日期之间相差天数.
     *
     * @param start 计算开始日期
     * @param end   计算结束日期
     * @return long 相隔天数
     */
    public static long getDaysBetween(Date start, Date end) {
        // 将指定日期转换为yyyy-MM-dd格式
        start = DateUtils.parseDate(DateUtils.formatDate(start, DateUtils.DATE_FORMAT));
        // 当前日期转换为yyyy-MM-dd格式
        end = DateUtils.parseDate(DateUtils.formatDate(end, DateUtils.DATE_FORMAT));
        long diff = 0;
        if (start != null && end != null) {
            diff = (end.getTime() - start.getTime()) / DateUtils.MILLISECONDS_PER_DAY;
        }
        return diff;
    }

    /**
     * 计算两个日期之前相隔多少周.
     *
     * @param start 计算开始时间
     * @param end   计算结束时间
     * @return long 相隔周数，向下取整
     */
    public static long getWeeksBetween(Date start, Date end) {
        return getDaysBetween(start, end) / DateUtils.DAYS_PER_WEEK;
    }

    /**
     * 获取与指定日期间隔给定天数的日期.
     *
     * @param specifiedDay 给定的字符串格式日期，支持的日期字符串格式包括"yyyy-MM-dd","yyyy-MM-dd HH:mm:ss",
     *                     "yyyy-MM-dd HH:mm", "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss",
     *                     "yyyy/MM/dd HH:mm"
     * @param num          间隔天数
     * @return String 间隔指定天数之后的日期
     */
    public static String getSpecifiedDayAfter(String specifiedDay, int num) {
        Date specifiedDate = parseDate(specifiedDay);
        Calendar c = Calendar.getInstance();
        c.setTime(specifiedDate);
        int day = c.get(Calendar.DATE);
        c.set(Calendar.DATE, day + num);
        String dayAfter = formatDate(c.getTime(), DateUtils.DATE_FORMAT);
        return dayAfter;
    }

    /**
     * 计算两个日期之前间隔的小时数.
     *
     * @param date1 结束时间
     * @param date2 开始时间
     * @return String 相差的小时数，保留一位小数
     */
    public static String dateMinus(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return "0";
        }
        long r = date1.getTime() - date2.getTime();
        DecimalFormat df = new DecimalFormat("#.0");
        double result = r * 1.0 / DateUtils.MILLISECONDS_PER_HOUR;
        return df.format(result);
    }

    /**
     * 将以秒为单位的时间转换为其他单位.
     *
     * @param seconds 秒数
     * @return String 例如 16分钟前、2小时前、3天前、4月前、5年前等
     */
    public static String getIntervalBySeconds(long seconds) {
        StringBuilder sb = new StringBuilder();
        if (seconds < SECONDS_PER_MINUTE) {
            sb.append(seconds).append("秒前");
        } else if (seconds < SECONDS_PER_HOUR) {
            sb.append(seconds / SECONDS_PER_MINUTE).append("分钟前");
        } else if (seconds < SECONDS_PER_DAY) {
            sb.append(seconds / SECONDS_PER_HOUR).append("小时前");
        } else if (seconds < SECONDS_PER_MONTH) {
            sb.append(seconds / SECONDS_PER_DAY).append("天前");
        } else if (seconds < SECONDS_PER_YEAR) {
            sb.append(seconds / SECONDS_PER_MONTH).append("月前");
        } else {
            sb.append(seconds / DateUtils.SECONDS_PER_YEAR).append("年前");
        }
        return sb.toString();
    }

    /**
     * getNowTimeBefore(记录时间相当于目前多久之前)
     *
     * @param seconds 秒
     * @return
     */
    public static String getNowTimeBefore(long seconds) {
        StringBuilder sb = new StringBuilder();
        sb.append("上传于");
        if (seconds < SECONDS_PER_HOUR) {
            sb.append((long) Math.floor(seconds / 60.0)).append("分钟前");
        } else if (seconds < 86400) {
            sb.append((long) Math.floor(seconds / 3600.0)).append("小时前");
        } else if (seconds < 604800) {
            sb.append((long) Math.floor(seconds / 86400.0)).append("天前");
        } else if (seconds < 2592000) {
            sb.append((long) Math.floor(seconds / 604800.0)).append("周前");
        } else if (seconds < 31104000) {
            sb.append((long) Math.floor(seconds / 2592000.0)).append("月前");
        } else {
            sb.append((long) Math.floor(seconds / 31104000.0)).append("年前");
        }
        return sb.toString();
    }

    /**
     * 获取当前日期是星期几
     *
     * @param dateStr 日期
     * @return 星期几
     */
    public static String getDayOfWeek(String dateStr) {
        String[] weekOfDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Date date = parseDate(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int num = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        return weekOfDays[num];
    }

    /**
     * 获取格式 如几秒前，几分钟前，几小时前，几天前，几个月前，几年后， ... 精细，类如某个明星几秒钟之前发表了一篇微博
     *
     * @param time 日期
     * @return 获取格式
     */
    public static String snsFormat(long time) {
        long now = System.currentTimeMillis() / 1000;
        long differ = now - time / 1000;
        String dateStr;
        if (differ <= SECONDS_PER_MINUTE) {
            dateStr = "刚刚";
        } else if (differ <= SECONDS_PER_HOUR) {
            dateStr = (differ / SECONDS_PER_MINUTE) + "分钟前";
        } else if (differ <= SECONDS_PER_HOUR * 24) {
            dateStr = (differ / SECONDS_PER_HOUR) + "小时前";
        } else if (differ <= SECONDS_PER_HOUR * 24 * 30) {
            dateStr = (differ / (SECONDS_PER_HOUR * 24)) + "天前";
        } else {
            Date date = new Date(time);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            dateStr = sdf.format(date);
        }
        return dateStr;
    }

    /**
     * 两个时间相差距离多少天多少小时多少分多少秒
     *
     * @param str1 时间参数 1 格式：1990-01-01 12:00:00
     * @param str2 时间参数 2 格式：2009-01-01 12:00:00
     * @return String 返回值为：xx天xx小时xx分xx秒
     */
    public static String getDistanceTime(String str1, String str2) {
        return getDistanceTime(str1, str2, true);
    }

    public static String getDistanceTime(String str1, String str2, boolean hasSec) {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        Date one;
        Date two;
        long day = 0;
        long hour = 0;
        long min = 0;
        long sec = 0;
        try {
            one = df.parse(str1);
            two = df.parse(str2);
            long time1 = one.getTime();
            long time2 = two.getTime();
            long diff;
            if (time1 < time2) {
                diff = time2 - time1;
            } else {
                diff = time1 - time2;
            }
            day = diff / (24 * 60 * 60 * 1000);
            hour = (diff / (60 * 60 * 1000) - day * 24);
            min = ((diff / (60 * 1000)) - day * 24 * 60 - hour * 60);
            sec = (diff / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (hasSec) {
            return day + "天" + hour + "小时" + min + "分" + sec + "秒";
        } else {
            return day + "天" + hour + "小时" + min + "分";
        }
    }

}
