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
     * 返回结构：环境 -> 服务 -> 元数据（host/logPath）。
     * </p>
     *
     * @return 服务概览
     */
    public Map<String, Map<String, Map<String, String>>> getServerOverview() {
        Map<String, Map<String, Map<String, String>>> result =
                new LinkedHashMap<>();
        Map<String, Map<String, LogPanelProperties.ServerNode>> all = safeServers();
        for (Map.Entry<String, Map<String, LogPanelProperties.ServerNode>> envEntry : all.entrySet()) {
            Map<String, Map<String, String>> serviceMap = new LinkedHashMap<>();
            Map<String, LogPanelProperties.ServerNode> services = safeServiceMap(envEntry.getValue());
            for (Map.Entry<String, LogPanelProperties.ServerNode> serviceEntry : services.entrySet()) {
                LogPanelProperties.ServerNode node = serviceEntry.getValue();
                Map<String, String> meta = new LinkedHashMap<>();
                meta.put("host", node == null ? "" : defaultString(node.getHost()));
                meta.put("logPath", node == null ? "" : defaultString(node.getLogPath()));
                serviceMap.put(serviceEntry.getKey(), meta);
            }
            result.put(envEntry.getKey(), serviceMap);
        }
        return result;
    }

    /**
     * 按环境与服务解析单个目标。
     *
     * @param env     环境名
     * @param service 服务名
     * @return 单个日志目标
     */
    public LogTarget resolveTarget(String env, String service) {
        String normalizedEnv = trimToEmpty(env);
        String normalizedService = trimToEmpty(service);
        if (normalizedEnv.isEmpty()) {
            throw new IllegalArgumentException("环境不能为空");
        }
        if (normalizedService.isEmpty()) {
            throw new IllegalArgumentException("服务不能为空");
        }
        Map<String, LogPanelProperties.ServerNode> serviceMap = safeServers().get(normalizedEnv);
        if (serviceMap == null) {
            throw new IllegalArgumentException("未找到环境: " + normalizedEnv);
        }
        LogPanelProperties.ServerNode node = serviceMap.get(normalizedService);
        if (node == null) {
            throw new IllegalArgumentException("环境[" + normalizedEnv + "]下未找到服务: " + normalizedService);
        }
        return new LogTarget(normalizedEnv, normalizedService, node);
    }

    /**
     * 按请求解析订阅目标列表，支持“全部环境/全部服务”聚合。
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

        boolean allEnv = request.isAllEnv();
        boolean allService = request.isAllService();
        String normalizedEnv = request.normalizedEnv();
        String normalizedService = request.normalizedService();

        List<LogTarget> targets = new ArrayList<>();
        if (allEnv && allService) {
            for (Map.Entry<String, Map<String, LogPanelProperties.ServerNode>> envEntry : all.entrySet()) {
                addAllServices(envEntry.getKey(), safeServiceMap(envEntry.getValue()), targets);
            }
        } else if (allEnv) {
            for (Map.Entry<String, Map<String, LogPanelProperties.ServerNode>> envEntry : all.entrySet()) {
                LogPanelProperties.ServerNode node = safeServiceMap(envEntry.getValue()).get(normalizedService);
                if (node != null) {
                    targets.add(new LogTarget(envEntry.getKey(), normalizedService, node));
                }
            }
        } else if (allService) {
            Map<String, LogPanelProperties.ServerNode> serviceMap = all.get(normalizedEnv);
            if (serviceMap == null) {
                throw new IllegalArgumentException("未找到环境: " + normalizedEnv);
            }
            addAllServices(normalizedEnv, serviceMap, targets);
        } else {
            targets.add(resolveTarget(normalizedEnv, normalizedService));
        }

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("未匹配到可订阅的环境/服务");
        }
        return targets;
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
