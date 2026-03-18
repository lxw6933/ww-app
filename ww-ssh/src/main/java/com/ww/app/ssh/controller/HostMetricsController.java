package com.ww.app.ssh.controller;

import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.ssh.model.HostMetricSnapshot;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
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

/**
 * 主机指标查询接口。
 * <p>
 * 按"项目 + 环境 + 服务（支持全部服务聚合）"返回对应实例的实时主机指标。
 * 该接口是日志面板左侧指标栏的数据来源。
 * </p>
 * <p>
 * 性能优化说明：
 * 1. 多 target 并行采集：使用专用线程池并行，耗时从 N×T 降为 max(T)；<br>
 * 2. 单 target 合并命令：CPU/内存/交换/磁盘/负载 5次 SSH 合并为 1次，总 SSH 连接从 7 降为 3；<br>
 * 3. 短期缓存：5s 内同维度重复请求直接返回缓存，避免高频重复采集；<br>
 * 4. 缓存击穿防护：同 key 首次并发只触发一次采集，其余等待复用结果。<br>
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
     * 同一缓存 key 的首次请求会登记 Future 到此 Map；
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
     * <p>
     * 注意：该线程池只承载“单个 target 的 SSH 采集任务”，
     * 不再承载外层编排逻辑，避免出现同一线程池内嵌套提交并同步等待的阻塞问题。
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

            List<HostMetricSnapshot> metrics = collectWithInflightGuard(cacheKey, project, env, service);
            return new ArrayList<>(metrics);

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "查询主机指标失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 按缓存键执行采集，并复用同 key 的进行中任务。
     * <p>
     * 这里由当前请求线程负责“编排 + 等待结果”，
     * 专用线程池只负责单个 target 的 SSH 采集任务，
     * 避免出现“外层任务占住线程池、内层任务继续投递同一线程池并同步等待”的嵌套阻塞。
     * </p>
     *
     * @param cacheKey 缓存键
     * @param project  项目
     * @param env      环境
     * @param service  服务
     * @return 指标列表
     * @throws Exception 采集异常
     */
    private List<HostMetricSnapshot> collectWithInflightGuard(
            String cacheKey, String project, String env, String service) throws Exception {
        CompletableFuture<List<HostMetricSnapshot>> newFuture = new CompletableFuture<>();
        CompletableFuture<List<HostMetricSnapshot>> existingFuture = inflightMap.putIfAbsent(cacheKey, newFuture);
        CompletableFuture<List<HostMetricSnapshot>> future = existingFuture == null ? newFuture : existingFuture;
        if (existingFuture == null) {
            try {
                List<HostMetricSnapshot> metrics = doCollect(project, env, service);
                metricsCache.put(cacheKey, new CacheEntry(System.currentTimeMillis(), metrics));
                future.complete(metrics);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
                throw ex;
            } finally {
                inflightMap.remove(cacheKey, future);
            }
        }
        return future.get(COLLECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 执行真正的 SSH 并行采集逻辑。
     * <p>
     * 对每个 target 提交独立任务到专用线程池，并使用统一超时等待全部任务。
     * 单个 target 采集失败或超时只会回填该节点的 error 快照，不影响其他 target。
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
        List<Callable<HostMetricSnapshot>> tasks = new ArrayList<>(targets.size());
        for (LogTarget target : targets) {
            tasks.add(() -> collectMetricSafely(target));
        }

        List<HostMetricSnapshot> metrics = new ArrayList<>(targets.size());
        try {
            List<Future<HostMetricSnapshot>> futures = metricsExecutor.invokeAll(
                    tasks, COLLECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (int i = 0; i < futures.size(); i++) {
                Future<HostMetricSnapshot> future = futures.get(i);
                LogTarget target = targets.get(i);
                if (future.isCancelled()) {
                    log.warn("主机指标采集超时: target={}", target.displayName());
                    metrics.add(buildErrorSnapshot(target, "采集超时"));
                    continue;
                }
                try {
                    HostMetricSnapshot snapshot = future.get();
                    metrics.add(snapshot == null ? buildErrorSnapshot(target, "采集结果为空") : snapshot);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    log.warn("主机指标采集任务异常: target={}, error={}", target.displayName(), cause.getMessage());
                    metrics.add(buildErrorSnapshot(target, "采集异常: " + cause.getMessage()));
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("主机指标采集被中断", ex);
        }

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
     * 安全采集单个目标的主机指标。
     *
     * @param target 目标节点
     * @return 指标快照
     */
    private HostMetricSnapshot collectMetricSafely(LogTarget target) {
        try {
            return sshLogService.queryHostMetric(target);
        } catch (Exception ex) {
            log.warn("采集主机指标失败: target={}, error={}", target.displayName(), ex.getMessage());
            return buildErrorSnapshot(target, "采集异常: " + ex.getMessage());
        }
    }

    /**
     * 构造错误快照，避免单个节点失败影响整体结果结构。
     *
     * @param target  目标节点
     * @param message 错误消息
     * @return 错误快照
     */
    private HostMetricSnapshot buildErrorSnapshot(LogTarget target, String message) {
        HostMetricSnapshot errSnapshot = new HostMetricSnapshot();
        errSnapshot.setProject(target == null ? "" : target.getProject());
        errSnapshot.setEnv(target == null ? "" : target.getEnv());
        errSnapshot.setService(target == null ? "" : target.getService());
        errSnapshot.setHost(target == null || target.getServerNode() == null
                ? "" : nullSafe(target.getServerNode().getHost()));
        errSnapshot.setStatus("error");
        errSnapshot.setMessage(message == null ? "采集失败" : message);
        return errSnapshot;
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
}
