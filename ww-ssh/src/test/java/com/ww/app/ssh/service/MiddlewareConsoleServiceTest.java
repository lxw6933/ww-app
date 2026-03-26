package com.ww.app.ssh.service;

import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.MiddlewareConsoleVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link MiddlewareConsoleService} 测试。
 */
class MiddlewareConsoleServiceTest {

    /**
     * 校验会按项目与环境返回共享的中间件后台列表。
     */
    @Test
    void shouldListEnvironmentSharedConsoles() {
        MiddlewareConsoleService service = new MiddlewareConsoleService(
                new LogPanelQueryService(mockPropertiesWithEnvironmentMiddlewares())
        );

        List<MiddlewareConsoleVO> consoles = service.listConsoles("ww-mall", "TEST-1", "mall-basic@node1");
        Assertions.assertEquals(3, consoles.size());
        Assertions.assertEquals("Nacos", consoles.get(0).getName());
        Assertions.assertEquals("rabbitmq", consoles.get(1).getCode());
        Assertions.assertEquals("admin", consoles.get(1).getUsername());
        Assertions.assertEquals("bad", consoles.get(2).getCode());
        Assertions.assertFalse(Boolean.TRUE.equals(consoles.get(2).getLaunchable()));
    }

    /**
     * 校验非法协议的中间件地址会被拒绝跳转。
     */
    @Test
    void shouldRejectLaunchWhenUrlSchemeIsInvalid() {
        MiddlewareConsoleService service = new MiddlewareConsoleService(
                new LogPanelQueryService(mockPropertiesWithEnvironmentMiddlewares())
        );

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.resolveLaunchUrl("ww-mall", "TEST-1", "mall-basic@node2", "bad"));
        Assertions.assertTrue(exception.getMessage().contains("非法"));
    }

    /**
     * 校验当环境级中间件未配置时，会兼容回退到服务级配置。
     */
    @Test
    void shouldFallbackToServiceLevelMiddlewaresWhenEnvIsNotConfigured() {
        MiddlewareConsoleService service = new MiddlewareConsoleService(
                new LogPanelQueryService(mockPropertiesWithServiceLevelFallback())
        );

        List<MiddlewareConsoleVO> consoles = service.listConsoles("ww-mall", "TEST-2", "mall-basic@node1");
        Assertions.assertEquals(1, consoles.size());
        Assertions.assertEquals("nacos", consoles.get(0).getCode());
        Assertions.assertEquals("nacos.fallback.local:8848/nacos", consoles.get(0).getUrl());
        Assertions.assertTrue(Boolean.TRUE.equals(consoles.get(0).getLaunchable()));
    }

    /**
     * 校验裸主机地址在跳转时会补全为浏览器可识别的 HTTP 地址。
     */
    @Test
    void shouldNormalizeBareHostWhenLaunching() {
        MiddlewareConsoleService service = new MiddlewareConsoleService(
                new LogPanelQueryService(mockPropertiesWithServiceLevelFallback())
        );

        String launchUrl = service.resolveLaunchUrl("ww-mall", "TEST-2", "mall-basic@node1", "nacos");
        Assertions.assertEquals("http://nacos.fallback.local:8848/nacos", launchUrl);
    }

    /**
     * 构造带环境级共享中间件配置的测试对象。
     *
     * @return 配置对象
     */
    private LogPanelProperties mockPropertiesWithEnvironmentMiddlewares() {
        LogPanelProperties properties = new LogPanelProperties();
        properties.setProjects(buildProjects("TEST-1", buildPlainNode("10.0.0.1"), buildPlainNode("10.0.0.2")));
        properties.setMiddlewares(buildEnvironmentMiddlewares());
        return properties;
    }

    /**
     * 构造仅保留服务级中间件配置的兼容测试对象。
     *
     * @return 配置对象
     */
    private LogPanelProperties mockPropertiesWithServiceLevelFallback() {
        LogPanelProperties properties = new LogPanelProperties();

        Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> projects = new LinkedHashMap<>();
        Map<String, Map<String, LogPanelProperties.ServerNode>> envs = new LinkedHashMap<>();
        Map<String, LogPanelProperties.ServerNode> services = new LinkedHashMap<>();
        services.put("mall-basic@node1", buildNodeWithServiceMiddlewares("10.0.1.1"));
        envs.put("TEST-2", services);
        projects.put("ww-mall", envs);
        properties.setProjects(projects);
        return properties;
    }

    /**
     * 构造项目与环境基础节点。
     *
     * @param env   环境名称
     * @param node1 第一个节点
     * @param node2 第二个节点
     * @return 项目配置
     */
    private Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> buildProjects(String env,
                                                                                                LogPanelProperties.ServerNode node1,
                                                                                                LogPanelProperties.ServerNode node2) {
        Map<String, Map<String, Map<String, LogPanelProperties.ServerNode>>> projects = new LinkedHashMap<>();
        Map<String, Map<String, LogPanelProperties.ServerNode>> envs = new LinkedHashMap<>();
        Map<String, LogPanelProperties.ServerNode> services = new LinkedHashMap<>();
        services.put("mall-basic@node1", node1);
        services.put("mall-basic@node2", node2);
        envs.put(env, services);
        projects.put("ww-mall", envs);
        return projects;
    }

    /**
     * 构造环境级共享中间件配置。
     *
     * @return 环境级中间件映射
     */
    private Map<String, Map<String, Map<String, LogPanelProperties.MiddlewareConsole>>> buildEnvironmentMiddlewares() {
        Map<String, Map<String, Map<String, LogPanelProperties.MiddlewareConsole>>> middlewares = new LinkedHashMap<>();
        Map<String, Map<String, LogPanelProperties.MiddlewareConsole>> envs = new LinkedHashMap<>();
        Map<String, LogPanelProperties.MiddlewareConsole> consoles = new LinkedHashMap<>();
        consoles.put("nacos", buildConsole("Nacos", "http://nacos.test.local/nacos", "nacos", "nacos", 10, true));
        consoles.put("rabbitmq", buildConsole("RabbitMQ", "https://mq.test.local", "admin", "admin123", 20, true));
        consoles.put("bad", buildConsole("Bad Console", "javascript:alert(1)", "root", "root", 30, true));
        consoles.put("disabled", buildConsole("Disabled", "http://disabled.test.local", "x", "y", 40, false));
        envs.put("TEST-1", consoles);
        middlewares.put("ww-mall", envs);
        return middlewares;
    }

    /**
     * 构造不带服务级中间件的普通节点。
     *
     * @param host 主机地址
     * @return 节点配置
     */
    private LogPanelProperties.ServerNode buildPlainNode(String host) {
        LogPanelProperties.ServerNode node = new LogPanelProperties.ServerNode();
        node.setHost(host);
        node.setPort(22);
        node.setUsername("root");
        node.setPassword("pwd");
        node.setLogPath("/data/logs");
        return node;
    }

    /**
     * 构造带服务级中间件配置的兼容节点。
     *
     * @param host 主机地址
     * @return 节点配置
     */
    private LogPanelProperties.ServerNode buildNodeWithServiceMiddlewares(String host) {
        LogPanelProperties.ServerNode node = buildPlainNode(host);
        Map<String, LogPanelProperties.MiddlewareConsole> middlewares = new LinkedHashMap<>();
        middlewares.put("nacos", buildConsole("Nacos", "nacos.fallback.local:8848/nacos", "nacos", "nacos", 10, true));
        node.setMiddlewares(middlewares);
        return node;
    }

    /**
     * 构造中间件后台配置。
     *
     * @param name     名称
     * @param url      地址
     * @param username 账号
     * @param password 密码
     * @param sort     排序
     * @param enabled  是否启用
     * @return 中间件配置
     */
    private LogPanelProperties.MiddlewareConsole buildConsole(String name,
                                                              String url,
                                                              String username,
                                                              String password,
                                                              Integer sort,
                                                              boolean enabled) {
        LogPanelProperties.MiddlewareConsole console = new LogPanelProperties.MiddlewareConsole();
        console.setName(name);
        console.setUrl(url);
        console.setUsername(username);
        console.setPassword(password);
        console.setSort(sort);
        console.setEnabled(enabled);
        return console;
    }
}
