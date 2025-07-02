package com.ww.app.redis.component.pvuv.report;

import com.ww.app.redis.component.pvuv.RedisPvUvManager;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redis PV/UV报表
 * 支持按天、周、月等不同时间维度生成统计报表
 */
@Slf4j
public class RedisPvUvReport {

    /**
     * Redis PV/UV管理器
     */
    private final RedisPvUvManager pvUvManager;

    /**
     * 并发查询线程池
     */
    private final ExecutorService queryExecutor;

    /**
     * 构造函数
     *
     * @param pvUvManager Redis PV/UV管理器
     */
    public RedisPvUvReport(RedisPvUvManager pvUvManager) {
        this.pvUvManager = Objects.requireNonNull(pvUvManager, "PvUvManager不能为空");
        // 创建守护线程的线程池，避免阻止JVM退出
        this.queryExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                r -> {
                    Thread thread = new Thread(r, "pv-uv-report-thread");
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    /**
     * 获取指定天数的每日PV报表
     *
     * @param key  统计键
     * @param days 天数
     * @return 每日PV报表
     */
    public Map<LocalDate, Long> getDailyPvReport(String key, int days) {
        return getDailyReport(key, days, date -> pvUvManager.getPv(key, date));
    }

    /**
     * 获取指定天数的每日UV报表
     *
     * @param key  统计键
     * @param days 天数
     * @return 每日UV报表
     */
    public Map<LocalDate, Long> getDailyUvReport(String key, int days) {
        return getDailyReport(key, days, date -> pvUvManager.getUv(key, date));
    }

    /**
     * 获取指定天数的每日PV/UV报表
     *
     * @param key  统计键
     * @param days 天数
     * @return 每日PV/UV报表
     */
    public Map<LocalDate, RedisPvUvManager.PvUvResult> getDailyPvUvReport(String key, int days) {
        return getDailyReport(key, days, date -> pvUvManager.getPvAndUv(key, date));
    }

    /**
     * 获取最近几周的每周PV统计
     *
     * @param key   统计键
     * @param weeks 周数
     * @return 每周PV统计
     */
    public Map<String, Long> getWeeklyPvReport(String key, int weeks) {
        if (key == null || weeks <= 0) {
            return new LinkedHashMap<>();
        }

        Map<String, Long> report = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = weeks - 1; i >= 0; i--) {
            // 计算第i周的开始和结束日期
            LocalDate endOfWeek = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
            LocalDate startOfWeek = endOfWeek.minusDays(6);
            String weekKey = startOfWeek + " ~ " + endOfWeek;

            // 使用并行查询提高性能
            long weeklyPv = getDaysInRange(startOfWeek, endOfWeek)
                    .parallelStream()
                    .mapToLong(date -> {
                        try {
                            return pvUvManager.getPv(key, date);
                        } catch (Exception e) {
                            log.warn("获取[{}]在[{}]的PV失败: {}", key, date, e.getMessage());
                            return 0;
                        }
                    })
                    .sum();

            report.put(weekKey, weeklyPv);
        }

        return report;
    }

    /**
     * 获取最近几周的每周UV统计
     *
     * @param key   统计键
     * @param weeks 周数
     * @return 每周UV统计
     */
    public Map<String, Long> getWeeklyUvReport(String key, int weeks) {
        if (key == null || weeks <= 0) {
            return new LinkedHashMap<>();
        }

        Map<String, Long> report = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = weeks - 1; i >= 0; i--) {
            // 计算第i周的开始和结束日期
            LocalDate endOfWeek = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
            LocalDate startOfWeek = endOfWeek.minusDays(6);
            String weekKey = startOfWeek + " ~ " + endOfWeek;

            // 使用并行查询提高性能
            long weeklyUv = getDaysInRange(startOfWeek, endOfWeek)
                    .parallelStream()
                    .mapToLong(date -> {
                        try {
                            return pvUvManager.getUv(key, date);
                        } catch (Exception e) {
                            log.warn("获取[{}]在[{}]的UV失败: {}", key, date, e.getMessage());
                            return 0;
                        }
                    })
                    .sum();

            report.put(weekKey, weeklyUv);
        }

        return report;
    }

    /**
     * 获取最近几个月的每月PV统计
     *
     * @param key    统计键
     * @param months 月数
     * @return 每月PV统计
     */
    public Map<String, Long> getMonthlyPvReport(String key, int months) {
        if (key == null || months <= 0) {
            return new LinkedHashMap<>();
        }

        Map<String, Long> report = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate date = today.minusMonths(i);
            LocalDate startOfMonth = date.withDayOfMonth(1);
            LocalDate endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
            String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());

            // 使用并行查询提高性能
            long monthlyPv = getDaysInRange(startOfMonth, endOfMonth)
                    .parallelStream()
                    .mapToLong(day -> {
                        try {
                            return pvUvManager.getPv(key, day);
                        } catch (Exception e) {
                            log.warn("获取[{}]在[{}]的PV失败: {}", key, day, e.getMessage());
                            return 0;
                        }
                    })
                    .sum();

            report.put(monthKey, monthlyPv);
        }

        return report;
    }

    /**
     * 获取最近几个月的每月UV统计
     *
     * @param key    统计键
     * @param months 月数
     * @return 每月UV统计
     */
    public Map<String, Long> getMonthlyUvReport(String key, int months) {
        if (key == null || months <= 0) {
            return new LinkedHashMap<>();
        }

        Map<String, Long> report = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate date = today.minusMonths(i);
            LocalDate startOfMonth = date.withDayOfMonth(1);
            LocalDate endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
            String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());

            // 使用并行查询提高性能
            long monthlyUv = getDaysInRange(startOfMonth, endOfMonth)
                    .parallelStream()
                    .mapToLong(day -> {
                        try {
                            return pvUvManager.getUv(key, day);
                        } catch (Exception e) {
                            log.warn("获取[{}]在[{}]的UV失败: {}", key, day, e.getMessage());
                            return 0;
                        }
                    })
                    .sum();

            report.put(monthKey, monthlyUv);
        }

        return report;
    }

    /**
     * 获取指定日期范围的每日PV统计
     *
     * @param key       统计键
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日PV统计
     */
    public Map<LocalDate, Long> getRangePvReport(String key, LocalDate startDate, LocalDate endDate) {
        if (key == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return new LinkedHashMap<>();
        }

        return getDailyReport(key, startDate, endDate, date -> {
            try {
                return pvUvManager.getPv(key, date);
            } catch (Exception e) {
                log.warn("获取[{}]在[{}]的PV失败: {}", key, date, e.getMessage());
                return 0L;
            }
        });
    }

    /**
     * 获取指定日期范围的每日UV统计
     *
     * @param key       统计键
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日UV统计
     */
    public Map<LocalDate, Long> getRangeUvReport(String key, LocalDate startDate, LocalDate endDate) {
        if (key == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return new LinkedHashMap<>();
        }

        return getDailyReport(key, startDate, endDate, date -> {
            try {
                return pvUvManager.getUv(key, date);
            } catch (Exception e) {
                log.warn("获取[{}]在[{}]的UV失败: {}", key, date, e.getMessage());
                return 0L;
            }
        });
    }

    /**
     * 获取指定日期范围的每日PV/UV统计
     *
     * @param key       统计键
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日PV/UV统计
     */
    public Map<LocalDate, RedisPvUvManager.PvUvResult> getRangePvUvReport(String key, LocalDate startDate, LocalDate endDate) {
        if (key == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return new LinkedHashMap<>();
        }

        return getDailyReport(key, startDate, endDate, date -> {
            try {
                return pvUvManager.getPvAndUv(key, date);
            } catch (Exception e) {
                log.warn("获取[{}]在[{}]的PV/UV失败: {}", key, date, e.getMessage());
                return new RedisPvUvManager.PvUvResult(0, 0);
            }
        });
    }

    /**
     * 通用的每日报表生成方法
     *
     * @param key            统计键
     * @param days           天数
     * @param valueExtractor 值提取器
     * @param <T>            值类型
     * @return 报表
     */
    private <T> Map<LocalDate, T> getDailyReport(String key, int days, Function<LocalDate, T> valueExtractor) {
        if (key == null || days <= 0 || valueExtractor == null) {
            return new LinkedHashMap<>();
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        return getDailyReport(key, startDate, today, valueExtractor);
    }

    /**
     * 通用的日期范围报表生成方法
     *
     * @param key            统计键
     * @param startDate      开始日期
     * @param endDate        结束日期
     * @param valueExtractor 值提取器
     * @param <T>            值类型
     * @return 报表
     */
    private <T> Map<LocalDate, T> getDailyReport(String key, LocalDate startDate, LocalDate endDate,
                                                 Function<LocalDate, T> valueExtractor) {
        if (key == null || startDate == null || endDate == null || valueExtractor == null || startDate.isAfter(endDate)) {
            return new LinkedHashMap<>();
        }

        // 使用并发查询提升性能
        Map<LocalDate, CompletableFuture<T>> futures = new LinkedHashMap<>();

        // 创建每一天的异步任务
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            futures.put(currentDate, CompletableFuture.supplyAsync(
                    () -> valueExtractor.apply(currentDate), queryExecutor));
        }

        // 等待所有任务完成并收集结果
        return futures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                return entry.getValue().join();
                            } catch (Exception e) {
                                log.error("获取[{}]在[{}]的数据失败: {}", key, entry.getKey(), e.getMessage());
                                return null;
                            }
                        },
                        (v1, v2) -> v1,  // 合并器（不会用到）
                        LinkedHashMap::new  // 保持顺序
                ));
    }

    /**
     * 获取指定日期范围内的所有日期列表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日期列表
     */
    private List<LocalDate> getDaysInRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return java.util.Collections.emptyList();
        }

        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return dates;
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        if (queryExecutor != null && !queryExecutor.isShutdown()) {
            queryExecutor.shutdown();
        }
    }
} 