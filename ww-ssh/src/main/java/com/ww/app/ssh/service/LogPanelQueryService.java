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
     * 返回结构：环境 -> 服务组 -> 元数据（host/logPath/instances）。
     * </p>
     *
     * @return 服务概览
     */
    public Map<String, Map<String, Map<String, String>>> getServerOverview() {
        Map<String, Map<String, Map<String, String>>> result = new LinkedHashMap<>();
        Map<String, Map<String, LogPanelProperties.ServerNode>> all = safeServers();
        for (Map.Entry<String, Map<String, LogPanelProperties.ServerNode>> envEntry : all.entrySet()) {
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
                    serviceMap.put(groupName, meta);
                } else {
                    int instances = Integer.parseInt(defaultString(meta.get("instances")).isEmpty() ? "1" : meta.get("instances"));
                    meta.put("instances", String.valueOf(instances + 1));
                }
            }
            result.put(envEntry.getKey(), serviceMap);
        }
        return result;
    }

    /**
     * 按请求解析订阅目标列表。
     * <p>
     * 约束：
     * 1. 必须指定单一环境，不支持环境聚合；<br>
     * 2. 支持“全部服务聚合”；<br>
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
        Map<String, Map<String, LogPanelProperties.ServerNode>> all = safeServers();
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedEnv = request.normalizedEnv();
        String normalizedService = request.normalizedService();
        if (request.isAllEnv()) {
            throw new IllegalArgumentException("不支持环境聚合，请选择具体环境");
        }
        if (normalizedEnv.isEmpty()) {
            throw new IllegalArgumentException("环境不能为空");
        }
        Map<String, LogPanelProperties.ServerNode> serviceMap = safeServiceMap(all.get(normalizedEnv));
        if (serviceMap.isEmpty()) {
            throw new IllegalArgumentException("未找到环境: " + normalizedEnv);
        }

        List<LogTarget> targets = new ArrayList<>();
        if (request.isAllService()) {
            addAllServices(normalizedEnv, serviceMap, targets);
        } else {
            targets.addAll(resolveServiceTargets(normalizedEnv, serviceMap, normalizedService));
        }

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("未匹配到可订阅的环境/服务");
        }
        return targets;
    }

    /**
     * 按环境与服务组名解析目标列表。
     *
     * @param env     环境名
     * @param service 服务组名
     * @return 匹配目标列表
     */
    public List<LogTarget> resolveTargets(String env, String service) {
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv(env);
        request.setService(service);
        return resolveTargets(request);
    }

    /**
     * 按“环境 + 实例服务键”精确解析单目标。
     * <p>
     * 该方法不会执行服务组聚合，适用于实例级启停运维场景。
     * </p>
     *
     * @param env           环境名
     * @param serviceKey    实例服务键（配置中的原始 key）
     * @return 单实例目标
     */
    public LogTarget resolveExactTarget(String env, String serviceKey) {
        String normalizedEnv = trimToEmpty(env);
        String normalizedServiceKey = trimToEmpty(serviceKey);
        if (normalizedEnv.isEmpty()) {
            throw new IllegalArgumentException("环境不能为空");
        }
        if (normalizedServiceKey.isEmpty()) {
            throw new IllegalArgumentException("实例服务不能为空");
        }
        Map<String, Map<String, LogPanelProperties.ServerNode>> all = safeServers();
        Map<String, LogPanelProperties.ServerNode> serviceMap = safeServiceMap(all.get(normalizedEnv));
        if (serviceMap.isEmpty()) {
            throw new IllegalArgumentException("未找到环境: " + normalizedEnv);
        }
        LogPanelProperties.ServerNode node = serviceMap.get(normalizedServiceKey);
        if (node == null) {
            throw new IllegalArgumentException("未找到实例服务: " + normalizedServiceKey);
        }
        return new LogTarget(normalizedEnv, normalizedServiceKey, node);
    }

    /**
     * 安全获取全部配置，避免空指针。
     *
     * @return 环境服务配置映射
     */
    private Map<String, Map<String, LogPanelProperties.ServerNode>> safeServers() {
        Map<String, Map<String, LogPanelProperties.ServerNode>> servers = properties.getServers();
        return servers == null ? Collections.emptyMap() : servers;
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
     * @param env       环境名
     * @param serviceMap 服务映射
     * @param targets   目标收集器
     */
    private void addAllServices(String env,
                                Map<String, LogPanelProperties.ServerNode> serviceMap,
                                List<LogTarget> targets) {
        for (Map.Entry<String, LogPanelProperties.ServerNode> entry : serviceMap.entrySet()) {
            targets.add(new LogTarget(env, entry.getKey(), entry.getValue()));
        }
    }

    /**
     * 解析单服务（含多实例）目标集合。
     *
     * @param env        环境名
     * @param serviceMap 服务配置
     * @param service    服务组名
     * @return 匹配目标列表
     */
    private List<LogTarget> resolveServiceTargets(String env,
                                                  Map<String, LogPanelProperties.ServerNode> serviceMap,
                                                  String service) {
        String normalizedService = trimToEmpty(service);
        if (normalizedService.isEmpty()) {
            throw new IllegalArgumentException("服务不能为空");
        }
        List<LogTarget> targets = new ArrayList<>();
        for (Map.Entry<String, LogPanelProperties.ServerNode> entry : serviceMap.entrySet()) {
            if (sameServiceGroup(entry.getKey(), normalizedService)) {
                targets.add(new LogTarget(env, entry.getKey(), entry.getValue()));
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
     * 将空字符串标准化为""，避免 null 出现在 JSON 结果中。
     *
     * @param source 输入字符串
     * @return 非 null 字符串
     */
    private String defaultString(String source) {
        return source == null ? "" : source;
    }
}
