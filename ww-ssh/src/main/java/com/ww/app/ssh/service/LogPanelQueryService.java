package com.ww.app.ssh.service;

import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志面板配置查询服务。
 * <p>
 * 负责将原始配置转换为前端可用结构，并根据请求解析需要订阅的目标列表。
 * 当前支持“项目 -> 环境 -> 服务实例”三级维度。
 * </p>
 */
@Service
public class LogPanelQueryService {

    /**
     * 服务实例后缀分隔符。
     * <p>
     * 约定配置键形如：mall-basic@node1、mall-basic@node2。
     * 前端展示与查询使用“服务组名”（即 @ 前缀部分）。
     * </p>
     */
    private static final String SERVICE_INSTANCE_DELIMITER = "@";

    /**
     * 配置属性。
     */
    private final LogPanelProperties properties;

    /**
     * 构造方法。
     *
     * @param properties 日志面板配置属性
     */
    public LogPanelQueryService(LogPanelProperties properties) {
        this.properties = properties;
    }

    /**
     * 查询前端可消费的服务概览结构。
     * <p>
     * 返回结构：项目 -> 环境 -> 服务组 -> 元数据（host/logPath/instances）。
     * </p>
     *
     * @return 服务概览
     */
    public Map<String, Map<String, Map<String, Map<String, String>>>> getServerOverview() {
        Map<String, Map<String, Map<String, Map<String, String>>>> result = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> all = safeProjectServers();
        for (Map.Entry<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> projectEntry : all.entrySet()) {
            String project = projectEntry.getKey();
            Map<String, Map<String, Map<String, String>>> envMap = new LinkedHashMap<>();
            Map<String, Map<String, LogPanelProperties.ServerNode>> environments = safeEnvMap(projectEntry.getValue());
            for (Map.Entry<String, Map<String, LogPanelProperties.ServerNode>> envEntry : environments.entrySet()) {
                String env = envEntry.getKey();
                Map<String, Map<String, String>> serviceMap = new LinkedHashMap<>();
                Map<String, LogPanelProperties.ServerNode> services = safeServiceMap(envEntry.getValue());
                for (Map.Entry<String, LogPanelProperties.ServerNode> serviceEntry : services.entrySet()) {
                    String groupName = serviceGroupName(serviceEntry.getKey());
                    LogPanelProperties.ServerNode node = serviceEntry.getValue();
                    Map<String, String> meta = serviceMap.get(groupName);
                    if (meta == null) {
                        meta = new LinkedHashMap<>();
                        meta.put("host", node == null ? "" : defaultString(node.getHost()));
                        meta.put("logPath", node == null ? "" : defaultString(node.getLogPath()));
                        meta.put("instances", "1");
                        meta.put("targetType", resolveTargetType(node));
                        meta.put("supportsJvm", String.valueOf(supportsJvm(node)));
                        meta.put("supportsManage", String.valueOf(supportsManage(node)));
                        meta.put("supportsMiddleware", String.valueOf(supportsMiddleware(project, env, node)));
                        meta.put("middlewareCount", String.valueOf(middlewareCount(project, env, node)));
                        serviceMap.put(groupName, meta);
                    } else {
                        int instances = Integer.parseInt(defaultString(meta.get("instances")).isEmpty() ? "1" : meta.get("instances"));
                        meta.put("instances", String.valueOf(instances + 1));
                        meta.put("supportsJvm", String.valueOf(Boolean.parseBoolean(defaultString(meta.get("supportsJvm")))
                                || supportsJvm(node)));
                        meta.put("supportsManage", String.valueOf(Boolean.parseBoolean(defaultString(meta.get("supportsManage")))
                                || supportsManage(node)));
                        meta.put("supportsMiddleware", String.valueOf(Boolean.parseBoolean(defaultString(meta.get("supportsMiddleware")))
                                || supportsMiddleware(project, env, node)));
                        int currentMiddlewareCount = parseInt(defaultString(meta.get("middlewareCount")));
                        meta.put("middlewareCount", String.valueOf(Math.max(currentMiddlewareCount, middlewareCount(project, env, node))));
                    }
                }
                envMap.put(env, serviceMap);
            }
            result.put(project, envMap);
        }
        return result;
    }

    /**
     * 按请求解析订阅目标列表。
     * <p>
     * 约束：
     * 1. 必须指定单一项目与环境，不支持项目/环境聚合；
     * 2. 支持“全部服务聚合”；
     * 3. 支持“单服务多实例聚合”（同环境下 service@instance 会按 service 聚合）。
     * </p>
     *
     * @param request 日志订阅请求
     * @return 需要启动的目标列表
     */
    public List<LogTarget> resolveTargets(LogStreamRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> all = safeProjectServers();
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        if (request.isAllProject()) {
            throw new IllegalArgumentException("不支持项目聚合，请选择具体项目");
        }
        if (request.isAllEnv()) {
            throw new IllegalArgumentException("不支持环境聚合，请选择具体环境");
        }

        String normalizedProject = resolveProject(request.normalizedProject(), all);
        String normalizedEnv = request.normalizedEnv();
        String normalizedService = request.normalizedService();
        if (normalizedEnv.isEmpty()) {
            throw new IllegalArgumentException("环境不能为空");
        }

        Map<String, Map<String, LogPanelProperties.ServerNode>> envMap = safeEnvMap(all.get(normalizedProject));
        if (envMap.isEmpty()) {
            throw new IllegalArgumentException("未找到项目: " + normalizedProject);
        }
        Map<String, LogPanelProperties.ServerNode> serviceMap = safeServiceMap(envMap.get(normalizedEnv));
        if (serviceMap.isEmpty()) {
            throw new IllegalArgumentException("未找到环境: " + normalizedProject + "/" + normalizedEnv);
        }

        List<LogTarget> targets = new ArrayList<>();
        if (request.isAllService()) {
            addAllServices(normalizedProject, normalizedEnv, serviceMap, targets);
        } else {
            targets.addAll(resolveServiceTargets(normalizedProject, normalizedEnv, serviceMap, normalizedService));
        }

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("未匹配到可订阅的项目/环境/服务");
        }
        return targets;
    }

    /**
     * 按项目、环境与服务组名解析目标列表。
     *
     * @param project 项目名
     * @param env     环境名
     * @param service 服务组名
     * @return 匹配目标列表
     */
    public List<LogTarget> resolveTargets(String project, String env, String service) {
        LogStreamRequest request = new LogStreamRequest();
        request.setProject(project);
        request.setEnv(env);
        request.setService(service);
        return resolveTargets(request);
    }

    /**
     * 按“项目 + 环境 + 实例服务键”精确解析单目标。
     * <p>
     * 该方法不会执行服务组聚合，适用于实例级启停运维场景。
     * </p>
     *
     * @param project    项目名
     * @param env        环境名
     * @param serviceKey 实例服务键（配置中的原始 key）
     * @return 单实例目标
     */
    public LogTarget resolveExactTarget(String project, String env, String serviceKey) {
        Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> all = safeProjectServers();
        String normalizedProject = resolveProject(trimToEmpty(project), all);
        String normalizedEnv = trimToEmpty(env);
        String normalizedServiceKey = trimToEmpty(serviceKey);
        if (normalizedEnv.isEmpty()) {
            throw new IllegalArgumentException("环境不能为空");
        }
        if (normalizedServiceKey.isEmpty()) {
            throw new IllegalArgumentException("实例服务不能为空");
        }
        Map<String, Map<String, LogPanelProperties.ServerNode>> envMap = safeEnvMap(all.get(normalizedProject));
        if (envMap.isEmpty()) {
            throw new IllegalArgumentException("未找到项目: " + normalizedProject);
        }
        Map<String, LogPanelProperties.ServerNode> serviceMap = safeServiceMap(envMap.get(normalizedEnv));
        if (serviceMap.isEmpty()) {
            throw new IllegalArgumentException("未找到环境: " + normalizedProject + "/" + normalizedEnv);
        }
        LogPanelProperties.ServerNode node = serviceMap.get(normalizedServiceKey);
        if (node == null) {
            throw new IllegalArgumentException("未找到实例服务: " + normalizedServiceKey);
        }
        return new LogTarget(normalizedProject, normalizedEnv, normalizedServiceKey, node,
                resolveEnvironmentMiddlewareCount(normalizedProject, normalizedEnv, node));
    }

    /**
     * 查询指定项目与环境下的共享中间件配置。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @return 环境级共享中间件配置映射
     */
    public Map<String, LogPanelProperties.MiddlewareConsole> resolveEnvironmentMiddlewares(String project, String env) {
        Map<String, Map<String, Map<String, LogPanelProperties.MiddlewareConsole>>> all = safeProjectMiddlewares();
        String normalizedProject = trimToEmpty(project);
        String normalizedEnv = trimToEmpty(env);
        if (normalizedEnv.isEmpty()) {
            throw new IllegalArgumentException("环境不能为空");
        }
        if (normalizedProject.isEmpty()) {
            if (all.size() == 1) {
                normalizedProject = all.keySet().iterator().next();
            } else {
                normalizedProject = resolveProject(project, safeProjectServers());
            }
        }
        Map<String, Map<String, LogPanelProperties.MiddlewareConsole>> envMap = all.get(normalizedProject);
        if (envMap == null || envMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, LogPanelProperties.MiddlewareConsole> middlewares = envMap.get(normalizedEnv);
        return middlewares == null ? Collections.emptyMap() : middlewares;
    }

    /**
     * 解析有效项目名。
     * <p>
     * 当请求未显式传入项目且系统仅存在一个项目时，自动回填该项目；
     * 若存在多个项目则要求显式传值，避免误连错误环境。
     * </p>
     *
     * @param project 请求项目
     * @param all     全量项目配置
     * @return 解析后的项目名
     */
    private String resolveProject(String project,
                                  Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> all) {
        String normalizedProject = trimToEmpty(project);
        if (!normalizedProject.isEmpty()) {
            return normalizedProject;
        }
        if (all.size() == 1) {
            return all.keySet().iterator().next();
        }
        throw new IllegalArgumentException("项目不能为空");
    }

    /**
     * 安全获取全量“项目 -> 环境 -> 服务”配置，并兼容旧版结构。
     *
     * @return 非 null 的项目配置映射
     */
    private Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> safeProjectServers() {
        Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> merged = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> projects = properties.getProjects();
        if (projects != null) {
            merged.putAll(projects);
        }
        Map<String, Map<String, LogPanelProperties.ServerNode>> legacyServers = properties.getServers();
        if (legacyServers != null && !legacyServers.isEmpty()) {
            Map<String, Map<String, LogPanelProperties.ServerNode>> defaultProjectMap =
                    merged.computeIfAbsent(LogPanelProperties.DEFAULT_PROJECT, key -> new LinkedHashMap<>());
            for (Map.Entry<String, Map<String, LogPanelProperties.ServerNode>> envEntry : legacyServers.entrySet()) {
                Map<String, LogPanelProperties.ServerNode> exists = defaultProjectMap.get(envEntry.getKey());
                if (exists == null) {
                    defaultProjectMap.put(envEntry.getKey(), envEntry.getValue());
                } else {
                    exists.putAll(safeServiceMap(envEntry.getValue()));
                }
            }
        }
        return merged;
    }

    /**
     * 安全获取环境映射，避免空指针。
     *
     * @param envMap 环境映射
     * @return 非 null 映射
     */
    private Map<String, Map<String, LogPanelProperties.ServerNode>> safeEnvMap(
            Map<String, Map<String, LogPanelProperties.ServerNode>> envMap) {
        return envMap == null ? Collections.emptyMap() : envMap;
    }

    /**
     * 安全获取服务映射，避免空指针。
     *
     * @param serviceMap 服务映射
     * @return 非 null 映射
     */
    private Map<String, LogPanelProperties.ServerNode> safeServiceMap(
            Map<String, LogPanelProperties.ServerNode> serviceMap) {
        return serviceMap == null ? Collections.emptyMap() : serviceMap;
    }

    /**
     * 将某环境下全部服务加入目标列表。
     *
     * @param project    项目名
     * @param env        环境名
     * @param serviceMap 服务映射
     * @param targets    目标收集器
     */
    private void addAllServices(String project,
                                String env,
                                Map<String, LogPanelProperties.ServerNode> serviceMap,
                                List<LogTarget> targets) {
        for (Map.Entry<String, LogPanelProperties.ServerNode> entry : serviceMap.entrySet()) {
            targets.add(new LogTarget(project, env, entry.getKey(), entry.getValue(),
                    resolveEnvironmentMiddlewareCount(project, env, entry.getValue())));
        }
    }

    /**
     * 解析单服务（含多实例）目标集合。
     *
     * @param project    项目名
     * @param env        环境名
     * @param serviceMap 服务配置
     * @param service    服务组名
     * @return 匹配目标列表
     */
    private List<LogTarget> resolveServiceTargets(String project,
                                                  String env,
                                                  Map<String, LogPanelProperties.ServerNode> serviceMap,
                                                  String service) {
        String normalizedService = trimToEmpty(service);
        if (normalizedService.isEmpty()) {
            throw new IllegalArgumentException("服务不能为空");
        }
        List<LogTarget> targets = new ArrayList<>();
        for (Map.Entry<String, LogPanelProperties.ServerNode> entry : serviceMap.entrySet()) {
            if (sameServiceGroup(entry.getKey(), normalizedService)) {
                targets.add(new LogTarget(project, env, entry.getKey(), entry.getValue(),
                        resolveEnvironmentMiddlewareCount(project, env, entry.getValue())));
            }
        }
        return targets;
    }

    /**
     * 判断配置服务键与请求服务组是否属于同一服务。
     *
     * @param configuredService 配置服务键
     * @param requestService    请求服务组
     * @return true 表示同一服务组
     */
    private boolean sameServiceGroup(String configuredService, String requestService) {
        return serviceGroupName(configuredService).equals(requestService);
    }

    /**
     * 提取服务组名。
     *
     * @param serviceKey 配置中的服务键
     * @return 服务组名
     */
    private String serviceGroupName(String serviceKey) {
        String normalized = trimToEmpty(serviceKey);
        int delimiterIndex = normalized.indexOf(SERVICE_INSTANCE_DELIMITER);
        if (delimiterIndex <= 0) {
            return normalized;
        }
        return normalized.substring(0, delimiterIndex);
    }

    /**
     * 对字符串做去空格和 null 兜底。
     *
     * @param source 输入字符串
     * @return 非 null 字符串
     */
    private String trimToEmpty(String source) {
        return source == null ? "" : source.trim();
    }

    /**
     * 将空字符串标准化为 ""，避免 null 出现在 JSON 结果中。
     *
     * @param source 输入字符串
     * @return 非 null 字符串
     */
    private String defaultString(String source) {
        return source == null ? "" : source;
    }

    /**
     * 解析目标类型。
     *
     * @param node 节点配置
     * @return 目标类型
     */
    private String resolveTargetType(LogPanelProperties.ServerNode node) {
        if (node == null) {
            return LogPanelProperties.TARGET_TYPE_APP;
        }
        return node.normalizedTargetType();
    }

    /**
     * 判断目标是否支持 JVM 监控。
     *
     * @param node 节点配置
     * @return true 表示支持
     */
    private boolean supportsJvm(LogPanelProperties.ServerNode node) {
        return node != null && node.supportsJvmMonitor();
    }

    /**
     * 判断目标是否支持实例运维。
     *
     * @param node 节点配置
     * @return true 表示支持
     */
    private boolean supportsManage(LogPanelProperties.ServerNode node) {
        return node != null && !trimToEmpty(node.getManageCommandFile()).isEmpty();
    }

    /**
     * 判断目标是否已配置中间件后台入口。
     *
     * @param node 节点配置
     * @return true 表示已配置
     */
    private boolean supportsMiddleware(String project, String env, LogPanelProperties.ServerNode node) {
        return middlewareCount(project, env, node) > 0;
    }

    /**
     * 统计节点启用的中间件后台数量。
     *
     * @param node 节点配置
     * @return 数量
     */
    private int middlewareCount(String project, String env, LogPanelProperties.ServerNode node) {
        return resolveEnvironmentMiddlewareCount(project, env, node);
    }

    /**
     * 安全解析整型字符串。
     *
     * @param text 文本
     * @return 解析结果，失败时返回 0
     */
    private int parseInt(String text) {
        try {
            String normalized = trimToEmpty(text);
            if (normalized.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(normalized);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * 安全获取环境级共享中间件配置。
     *
     * @return 项目 -> 环境 -> 中间件 的配置映射
     */
    private Map<String, Map<String, Map<String, LogPanelProperties.MiddlewareConsole>>> safeProjectMiddlewares() {
        Map<String, Map<String, Map<String, LogPanelProperties.MiddlewareConsole>>> middlewares = properties.getMiddlewares();
        return middlewares == null ? Collections.emptyMap() : middlewares;
    }

    /**
     * 解析指定环境可用的中间件数量。
     * <p>
     * 优先使用环境级共享配置；若环境级未配置，则兼容回退到服务节点下的旧配置，
     * 以避免已有服务级配置在升级后立即失效。
     * </p>
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param node 服务节点配置
     * @return 中间件数量
     */
    private int resolveEnvironmentMiddlewareCount(String project, String env, LogPanelProperties.ServerNode node) {
        Map<String, LogPanelProperties.MiddlewareConsole> environmentMiddlewares =
                resolveEnvironmentMiddlewares(project, env);
        if (environmentMiddlewares != null && !environmentMiddlewares.isEmpty()) {
            return enabledMiddlewareCount(environmentMiddlewares);
        }
        return node == null ? 0 : node.middlewareCount();
    }

    /**
     * 统计中间件配置中启用项数量。
     *
     * @param middlewares 中间件配置映射
     * @return 启用数量
     */
    private int enabledMiddlewareCount(Map<String, LogPanelProperties.MiddlewareConsole> middlewares) {
        if (middlewares == null || middlewares.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (LogPanelProperties.MiddlewareConsole console : middlewares.values()) {
            if (console != null && console.isConsoleEnabled()) {
                count++;
            }
        }
        return count;
    }
}
