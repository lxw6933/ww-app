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
     * 校验全部环境+全部服务时会返回完整目标集合。
     */
    @Test
    void shouldResolveAllTargetsWhenAllEnvAndAllService() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv(LogStreamRequest.ALL);
        request.setService(LogStreamRequest.ALL);

        List<LogTarget> targets = service.resolveTargets(request);
        Assertions.assertEquals(3, targets.size());
    }

    /**
     * 校验“全部环境 + 指定服务”时会跨环境聚合同名服务。
     */
    @Test
    void shouldResolveSameServiceAcrossEnvironments() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv(LogStreamRequest.ALL);
        request.setService("mall-basic");

        List<LogTarget> targets = service.resolveTargets(request);
        Assertions.assertEquals(2, targets.size());
        Assertions.assertEquals("mall-basic", targets.get(0).getService());
        Assertions.assertEquals("mall-basic", targets.get(1).getService());
    }

    /**
     * 校验“指定环境 + 全部服务”时仅返回该环境下服务集合。
     */
    @Test
    void shouldResolveAllServicesInOneEnvironment() {
        LogPanelQueryService service = new LogPanelQueryService(mockProperties());
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv("TEST-1");
        request.setService(LogStreamRequest.ALL);

        List<LogTarget> targets = service.resolveTargets(request);
        Assertions.assertEquals(2, targets.size());
        Assertions.assertEquals("TEST-1", targets.get(0).getEnv());
        Assertions.assertEquals("TEST-1", targets.get(1).getEnv());
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
     * 组装测试用配置。
     *
     * @return 配置对象
     */
    private LogPanelProperties mockProperties() {
        LogPanelProperties properties = new LogPanelProperties();
        Map<String, Map<String, LogPanelProperties.ServerNode>> servers =
                new LinkedHashMap<>();

        Map<String, LogPanelProperties.ServerNode> test1 = new LinkedHashMap<>();
        test1.put("mall-basic", node("10.0.0.1", "/data/basic/logs"));
        test1.put("mall-auth", node("10.0.0.2", "/data/auth/logs"));
        servers.put("TEST-1", test1);

        Map<String, LogPanelProperties.ServerNode> test2 = new LinkedHashMap<>();
        test2.put("mall-basic", node("10.0.0.3", "/data/basic/logs"));
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
