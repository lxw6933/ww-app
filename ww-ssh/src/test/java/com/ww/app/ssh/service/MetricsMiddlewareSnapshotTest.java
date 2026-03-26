package com.ww.app.ssh.service;

import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.HostMetricSnapshot;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.support.LogLineFilterMatcher;
import com.ww.app.ssh.service.support.SshCommandBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * 主机指标快照中的中间件能力透出测试。
 * <p>
 * 该测试聚焦“环境级共享中间件”是否能正确进入指标快照，
 * 避免前端标题区入口因后端字段回退到服务级配置而不展示。
 * </p>
 */
class MetricsMiddlewareSnapshotTest {

    /**
     * 校验主机指标初始化快照会使用目标上已解析好的环境级中间件数量。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldExposeEnvironmentMiddlewareOnInitMetricSnapshot() throws Exception {
        SshLogService sshLogService = new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher());
        LogTarget target = buildTargetWithEnvironmentMiddlewareCount(3);

        Method method = SshLogService.class.getDeclaredMethod("initMetricSnapshot", LogTarget.class);
        method.setAccessible(true);
        HostMetricSnapshot snapshot = (HostMetricSnapshot) method.invoke(sshLogService, target);

        Assertions.assertTrue(Boolean.TRUE.equals(snapshot.getCanOpenMiddleware()));
        Assertions.assertEquals(3, snapshot.getMiddlewareCount());
    }

    /**
     * 校验错误快照同样会使用环境级中间件数量，避免采集失败时入口消失。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldExposeEnvironmentMiddlewareOnErrorSnapshot() throws Exception {
        MetricsSnapshotService metricsSnapshotService = new MetricsSnapshotService(
                new LogPanelQueryService(new LogPanelProperties()),
                new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher())
        );
        LogTarget target = buildTargetWithEnvironmentMiddlewareCount(2);

        Method method = MetricsSnapshotService.class.getDeclaredMethod(
                "buildErrorSnapshot", LogTarget.class, String.class);
        method.setAccessible(true);
        HostMetricSnapshot snapshot = (HostMetricSnapshot) method.invoke(
                metricsSnapshotService, target, "采集失败");

        Assertions.assertTrue(Boolean.TRUE.equals(snapshot.getCanOpenMiddleware()));
        Assertions.assertEquals(2, snapshot.getMiddlewareCount());
    }

    /**
     * 构造一个仅通过目标对象携带环境级中间件数量的测试节点。
     * <p>
     * 节点本身不挂任何服务级中间件配置，用于验证快照逻辑不会错误回退到
     * {@link LogPanelProperties.ServerNode#middlewareCount()}。
     * </p>
     *
     * @param middlewareCount 环境级中间件数量
     * @return 测试目标
     */
    private LogTarget buildTargetWithEnvironmentMiddlewareCount(int middlewareCount) {
        LogPanelProperties.ServerNode node = new LogPanelProperties.ServerNode();
        node.setHost("10.0.0.1");
        node.setPort(22);
        node.setUsername("root");
        node.setPassword("pwd");
        node.setLogPath("/data/logs");
        return new LogTarget("mall", "test", "mall-basic@node1", node, middlewareCount);
    }
}
