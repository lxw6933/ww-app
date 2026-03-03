package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.HostMetricSnapshot;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机指标查询接口。
 * <p>
 * 按“环境 + 服务（支持全部服务聚合）”返回对应实例的实时主机指标。
 * 该接口是日志面板左侧指标栏的数据来源。
 * </p>
 */
@RestController
@RequestMapping("/api/metrics")
public class HostMetricsController {

    /**
     * 指标缓存有效期（毫秒）。
     */
    private static final long CACHE_TTL_MS = 5000L;

    /**
     * 配置查询服务。
     */
    private final LogPanelQueryService logPanelQueryService;

    /**
     * SSH 日志服务（复用其 SSH 执行能力采集指标）。
     */
    private final SshLogService sshLogService;

    /**
     * 指标响应缓存。
     * <p>
     * 用于降低多人同时访问时的 SSH 采集压力，
     * 以“环境 + 服务”为粒度做短期缓存，避免高频重复采集。
     * </p>
     */
    private final Map<String, CacheEntry> metricsCache = new ConcurrentHashMap<>();

    /**
     * 构造方法。
     *
     * @param logPanelQueryService 配置查询服务
     * @param sshLogService        SSH 日志服务
     */
    public HostMetricsController(LogPanelQueryService logPanelQueryService, SshLogService sshLogService) {
        this.logPanelQueryService = logPanelQueryService;
        this.sshLogService = sshLogService;
    }

    /**
     * 查询主机指标列表。
     * <p>
     * 返回结果会按“状态优先 + 服务名 + 主机名”排序：
     * 1. 异常节点优先，便于快速发现问题；
     * 2. 同状态下按服务与主机稳定排序。
     * </p>
     *
     * @param env     环境名称（必填）
     * @param service 服务名称（支持“全部服务”占位值）
     * @return 主机指标快照列表
     */
    @GetMapping("/hosts")
    public List<HostMetricSnapshot> listHostMetrics(@RequestParam("env") String env,
                                                    @RequestParam("service") String service) {
        try {
            String cacheKey = buildCacheKey(env, service);
            long now = System.currentTimeMillis();
            CacheEntry cached = metricsCache.get(cacheKey);
            if (cached != null && now - cached.timestamp <= CACHE_TTL_MS) {
                return new ArrayList<>(cached.metrics);
            }

            List<LogTarget> targets = logPanelQueryService.resolveTargets(env, service);
            List<HostMetricSnapshot> metrics = new ArrayList<>();
            for (LogTarget target : targets) {
                metrics.add(sshLogService.queryHostMetric(target));
            }

            metrics.sort(Comparator
                    .comparing((HostMetricSnapshot metric) -> "ok".equals(metric.getStatus()) ? 1 : 0)
                    .thenComparing(metric -> nullSafe(metric.getService()))
                    .thenComparing(metric -> nullSafe(metric.getHost())));
            metricsCache.put(cacheKey, new CacheEntry(now, new ArrayList<>(metrics)));
            return metrics;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "查询主机指标失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 构建缓存键。
     *
     * @param env     环境
     * @param service 服务
     * @return 缓存键
     */
    private String buildCacheKey(String env, String service) {
        return nullSafe(env) + "|" + nullSafe(service);
    }

    /**
     * 字符串空值兜底。
     *
     * @param source 输入字符串
     * @return 非 null 字符串
     */
    private String nullSafe(String source) {
        return source == null ? "" : source;
    }

    /**
     * 指标缓存条目。
     */
    private static class CacheEntry {

        /**
         * 缓存时间戳。
         */
        private final long timestamp;

        /**
         * 缓存指标列表。
         */
        private final List<HostMetricSnapshot> metrics;

        /**
         * 构造方法。
         *
         * @param timestamp 时间戳
         * @param metrics   指标列表
         */
        private CacheEntry(long timestamp, List<HostMetricSnapshot> metrics) {
            this.timestamp = timestamp;
            this.metrics = metrics;
        }
    }
}
