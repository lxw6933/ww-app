package com.ww.app.ssh.controller;

import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.ssh.model.HostMetricSnapshot;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 主机指标查询接口。
 * <p>
 * 按"项目 + 环境 + 服务（支持全部服务聚合）"返回对应实例的实时主机指标。
 * 该接口是日志面板左侧指标栏的数据来源。
 * </p>
 * <p>
 * 性能优化说明：
 * 1. 多 target 并行采集：使用专用线程池 CompletableFuture 并行，耗时从 N×T 降为 max(T)；<br>
 * 2. 单 target 合并命令：CPU/内存/交换/磁盘/负载 5次 SSH 合并为 1次，总 SSH 连接从 7 降为 3；<br>
 * 3. 短期缓存：5s 内同维度重复请求直接返回缓存，避免高频重复采集；<br>
 * 4. 缓存击穿防护：同 key 首次并发只触发一次采集，其余等待复用结果。
 * </p>
 *
 * @author ww
 * @create 2026-03-12 14:38
 * {@code @description} 主机指标查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
public class HostMetricsController {

    /**
     * 指标缓存有效期（毫秒）。
     */
    private static final long CACHE_TTL_MS = 5000L;

    /**
     * 单次并行采集的超时时间（秒）。
     * <p>
     * 保证接口最长阻塞时间 = SSH 超时（默认 8s）+ 少量处理时间，此处设 30s 兜底。
     * </p>
     */
    private static final long COLLECT_TIMEOUT_SECONDS = 30L;

    /**
     * 指标采集线程池核心线程数。
     */
    private static final int METRICS_CORE_THREADS = 8;

    /**
     * 指标采集线程池最大线程数。
     */
    private static final int METRICS_MAX_THREADS = 32;

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
     * 以"项目 + 环境 + 服务"为粒度做短期缓存，避免高频重复采集。
     * </p>
     */
    private final Map<String, CacheEntry> metricsCache = new ConcurrentHashMap<>();

    /**
     * 并行采集任务缓存（防缓存击穿）。
     * <p>
     * 同一缓存 key 的首次请求会提交 CompletableFuture 到此 Map；
     * 并发请求直接复用同一个 Future，保证多个并发请求只触发一次 SSH 采集。
     * </p>
     */
    private final Map<String, CompletableFuture<List<HostMetricSnapshot>>> inflightMap = new ConcurrentHashMap<>();

    /**
     * 指标采集专用线程池。
     * <p>
     * 采用有界队列 + CallerRuns 拒绝策略，防止瞬时流量打爆线程池。
     * 核心线程数 {@value METRICS_CORE_THREADS}，最大 {@value METRICS_MAX_THREADS}，
     * 空闲 60s 回收。
     * </p>
     */
    private final ExecutorService metricsExecutor = ThreadUtil.initThreadPoolExecutor(
            "server-metrics",
            METRICS_CORE_THREADS,
            METRICS_MAX_THREADS,
            60,
            TimeUnit.SECONDS,
            256,
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

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
     * 返回结果会按"状态优先 + 服务名 + 主机名"排序：
     * 1. 异常节点优先，便于快速发现问题；
     * 2. 同状态下按服务与主机稳定排序。
     * </p>
     *
     * @param project 项目名称（必填）
     * @param env     环境名称（必填）
     * @param service 服务名称（支持"全部服务"占位值）
     * @return 主机指标快照列表
     */
    @GetMapping("/hosts")
    public List<HostMetricSnapshot> listHostMetrics(@RequestParam("project") String project,
                                                    @RequestParam("env") String env,
                                                    @RequestParam("service") String service) {
        try {
            String cacheKey = buildCacheKey(project, env, service);
            long now = System.currentTimeMillis();

            // 命中有效缓存直接返回
            CacheEntry cached = metricsCache.get(cacheKey);
            if (cached != null && now - cached.timestamp <= CACHE_TTL_MS) {
                return new ArrayList<>(cached.metrics);
            }

            // 防缓存击穿：同 key 并发只触发一次采集
            CompletableFuture<List<HostMetricSnapshot>> future = inflightMap.computeIfAbsent(
                    cacheKey, key -> buildCollectFuture(key, project, env, service));

            List<HostMetricSnapshot> metrics = future.get(COLLECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new ArrayList<>(metrics);

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "查询主机指标失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 构建并提交并行采集任务，完成后刷新缓存并从 inflightMap 中移除自身。
     *
     * @param cacheKey key（用于缓存写入和 inflightMap 清理）
     * @param project  项目
     * @param env      环境
     * @param service  服务
     * @return 采集任务 Future
     */
    private CompletableFuture<List<HostMetricSnapshot>> buildCollectFuture(
            String cacheKey, String project, String env, String service) {
        return CompletableFuture
                .supplyAsync(() -> doCollect(project, env, service), metricsExecutor)
                .whenComplete((result, ex) -> {
                    // 无论成功或失败，都从 inflightMap 中移除，避免错误结果被持久缓存
                    inflightMap.remove(cacheKey);
                    if (result != null) {
                        metricsCache.put(cacheKey, new CacheEntry(System.currentTimeMillis(), result));
                    }
                });
    }

    /**
     * 执行真正的 SSH 并行采集逻辑。
     * <p>
     * 对每个 target 提交独立的 CompletableFuture，并行采集后合并结果并排序。
     * 单个 target 采集失败会返回 status=error 的快照，不影响其他 target。
     * </p>
     *
     * @param project 项目
     * @param env     环境
     * @param service 服务
     * @return 排序后的指标快照列表
     */
    private List<HostMetricSnapshot> doCollect(String project, String env, String service) {
        List<LogTarget> targets = logPanelQueryService.resolveTargets(project, env, service);
        if (targets == null || targets.isEmpty()) {
            return new ArrayList<>();
        }

        long startMs = System.currentTimeMillis();

        // 并行提交各 target 的采集任务
        List<CompletableFuture<HostMetricSnapshot>> futures = targets.stream()
                .map(target -> CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return sshLogService.queryHostMetric(target);
                            } catch (Exception ex) {
                                log.warn("采集主机指标失败: target={}, error={}", target.displayName(), ex.getMessage());
                                // queryHostMetric 内部已兜底，此处作为双重保护
                                HostMetricSnapshot errSnapshot = new HostMetricSnapshot();
                                errSnapshot.setProject(target.getProject());
                                errSnapshot.setEnv(target.getEnv());
                                errSnapshot.setService(target.getService());
                                errSnapshot.setHost(target.getServerNode() == null
                                        ? "" : nullSafe(target.getServerNode().getHost()));
                                errSnapshot.setStatus("error");
                                errSnapshot.setMessage("采集异常: " + ex.getMessage());
                                return errSnapshot;
                            }
                        }, metricsExecutor))
                .collect(Collectors.toList());

        // 等待全部 target 完成并收集结果
        List<HostMetricSnapshot> metrics = futures.stream()
                .map(f -> {
                    try {
                        return f.get(COLLECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception ex) {
                        log.warn("等待主机指标采集超时或异常: {}", ex.getMessage());
                        return null;
                    }
                })
                .filter(snapshot -> snapshot != null)
                .collect(Collectors.toCollection(ArrayList::new));

        log.debug("并行采集主机指标完成: targets={}, elapsed={}ms", targets.size(),
                System.currentTimeMillis() - startMs);

        // 按"异常优先 + 服务名 + 主机名"稳定排序
        metrics.sort(Comparator
                .comparing((HostMetricSnapshot metric) -> "ok".equals(metric.getStatus()) ? 1 : 0)
                .thenComparing(metric -> nullSafe(metric.getService()))
                .thenComparing(metric -> nullSafe(metric.getHost())));

        return metrics;
    }

    /**
     * 应用关闭前优雅停止采集线程池。
     */
    @PreDestroy
    public void shutdown() {
        metricsExecutor.shutdown();
        try {
            if (!metricsExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                metricsExecutor.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            metricsExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 构建缓存键。
     *
     * @param project 项目
     * @param env     环境
     * @param service 服务
     * @return 缓存键
     */
    private String buildCacheKey(String project, String env, String service) {
        return nullSafe(project) + "|" + nullSafe(env) + "|" + nullSafe(service);
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

    /**
     * 指标采集线程工厂，提供有辨识度的线程名。
     */
    private static class MetricsThreadFactory implements ThreadFactory {

        /**
         * 线程编号序列。
         */
        private final AtomicInteger sequence = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable, "ww-metrics-collector-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
