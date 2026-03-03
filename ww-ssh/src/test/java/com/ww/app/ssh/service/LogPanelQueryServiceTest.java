package com.ww.app.ssh.service;

import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link LogPanelQueryService} 目标解析测试。
 */
class LogPanelQueryServiceTest {

    /**
     * 校验环境聚合被禁用。
     */
    @Test
    void shouldRejectWhenAllEnvironmentSelected() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv(LogStreamRequest.ALL);
        request.setService("mall-basic");
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.resolveTargets(request));
    }

    /**
     * 校验“指定环境 + 全部服务”时仅返回该环境下全部节点。
     */
    @Test
    void shouldResolveAllServicesInOneEnvironment() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv("TEST-1");
        request.setService(LogStreamRequest.ALL);

        List<LogTarget> targets = service.resolveTargets(request);
        Assertions.assertEquals(3, targets.size());
        Assertions.assertEquals("TEST-1", targets.get(0).getEnv());
    }

    /**
     * 校验“单服务多实例聚合”。
     */
    @Test
    void shouldResolveServiceGroupInstancesInOneEnvironment() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv("TEST-1");
        request.setService("mall-basic");

        List<LogTarget> targets = service.resolveTargets(request);
        Assertions.assertEquals(2, targets.size());
        Assertions.assertEquals("mall-basic@node1", targets.get(0).getService());
        Assertions.assertEquals("mall-basic@node2", targets.get(1).getService());
    }

    /**
     * 校验不存在环境时会抛出明确异常。
     */
    @Test
    void shouldThrowWhenEnvironmentNotFound() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv("NOT-EXIST");
        request.setService(LogStreamRequest.ALL);

        Assertions.assertThrows(IllegalArgumentException.class, () -> service.resolveTargets(request));
    }

    /**
     * 校验前端服务概览会按服务组去重展示。
     */
    @Test
    void shouldGroupServicesInServerOverview() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        Map<String, Map<String, Map<String, String>>> overview = service.getServerOverview();
        Map<String, Map<String, String>> test1 = overview.get("TEST-1");
        Assertions.assertNotNull(test1);
        Assertions.assertTrue(test1.containsKey("mall-basic"));
        Assertions.assertTrue(test1.containsKey("mall-auth"));
        Assertions.assertEquals("2", test1.get("mall-basic").get("instances"));
    }

    /**
     * 组装测试用配置。
     *
     * @return 配置对象
     */
    private LogPanelProperties mockProperties() {
        LogPanelProperties properties = new LogPanelProperties();
        Map<String, Map<String, LogPanelProperties.ServerNode>> servers =
                new LinkedHashMap<>();

        Map<String, LogPanelProperties.ServerNode> test1 = new LinkedHashMap<>();
        test1.put("mall-basic@node1", node("10.0.0.1", "/data/basic/logs"));
        test1.put("mall-basic@node2", node("10.0.0.2", "/data/basic/logs"));
        test1.put("mall-auth", node("10.0.0.3", "/data/auth/logs"));
        servers.put("TEST-1", test1);

        Map<String, LogPanelProperties.ServerNode> test2 = new LinkedHashMap<>();
        test2.put("mall-basic@node1", node("10.0.0.4", "/data/basic/logs"));
        servers.put("TEST-2", test2);

        properties.setServers(servers);
        return properties;
    }

    /**
     * 创建节点对象。
     *
     * @param host    主机地址
     * @param logPath 日志目录
     * @return 节点对象
     */
    private LogPanelProperties.ServerNode node(String host, String logPath) {
        LogPanelProperties.ServerNode node = new LogPanelProperties.ServerNode();
        node.setHost(host);
        node.setPort(22);
        node.setUsername("root");
        node.setPassword("pwd");
        node.setLogPath(logPath);
        return node;
    }
}
