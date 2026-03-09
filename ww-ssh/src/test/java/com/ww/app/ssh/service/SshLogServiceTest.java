package com.ww.app.ssh.service;

import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.service.support.LogLineFilterMatcher;
import com.ww.app.ssh.service.support.SshCommandBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link SshLogService} cat 快照扫描窗口策略测试。
 * <p>
 * 该测试聚焦“过滤场景自动扩大扫描窗口”逻辑，
 * 保障小行数配置（如 200）在内容过滤时不会因扫描窗口过小而误判为无命中。
 * </p>
 */
class SshLogServiceTest {

    /**
     * 被测对象。
     */
    private final SshLogService sshLogService =
            new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher());

    /**
     * 无过滤条件时应保持历史行为：扫描窗口等于请求行数。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldKeepRequestedWindowWhenNoFilterRules() throws Exception {
        int scanLines = invokeResolveCatScanLines(200, Collections.emptyList());
        Assertions.assertEquals(200, scanLines);
    }

    /**
     * 有过滤条件且请求行数较小时，应按倍数放大扫描窗口。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldEnlargeScanWindowWhenFilterRulesPresent() throws Exception {
        int scanLines = invokeResolveCatScanLines(200, Collections.singletonList(rule("include", "ERROR")));
        Assertions.assertEquals(1000, scanLines);
    }

    /**
     * 有过滤条件且放大后超上限时，应限制在 5000 行以内。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldCapScanWindowAtUpperBoundWhenFilterRulesPresent() throws Exception {
        int scanLines = invokeResolveCatScanLines(1200, Collections.singletonList(rule("include", "ERROR")));
        Assertions.assertEquals(5000, scanLines);
    }

    /**
     * 校验包含规则识别逻辑，确保包含条件触发全文件预筛路径。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldRecognizeIncludeRule() throws Exception {
        boolean includeMatched = invokeHasIncludeFilterRule(Collections.singletonList(rule("include", "ERROR")));
        boolean onlyExclude = invokeHasIncludeFilterRule(Collections.singletonList(rule("exclude", "DEBUG")));
        Assertions.assertTrue(includeMatched);
        Assertions.assertFalse(onlyExclude);
    }

    /**
     * 校验默认文件选择逻辑：info 日志优先于其他类型日志。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldPreferInfoFileWhenResolvingDefaultLogFile() throws Exception {
        String resolved = invokeResolvePreferredLogFilePath(Arrays.asList(
                "/data/logs/service-job.log",
                "/data/logs/service-error.log",
                "/data/logs/service-info.log"
        ));
        Assertions.assertEquals("/data/logs/service-info.log", resolved);
    }

    /**
     * 校验同优先级文件的比较逻辑：按完整路径倒序选择。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldChooseDescendingPathWhenPriorityIsSame() throws Exception {
        String resolved = invokeResolvePreferredLogFilePath(Arrays.asList(
                "/data/logs/service-debug-20260308.log",
                "/data/logs/service-debug-20260309.log"
        ));
        Assertions.assertEquals("/data/logs/service-debug-20260309.log", resolved);
    }

    /**
     * 校验候选为空时返回空字符串，供上层执行 latest 回退逻辑。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldReturnEmptyWhenNoCandidateLogFiles() throws Exception {
        String resolved = invokeResolvePreferredLogFilePath(Collections.emptyList());
        Assertions.assertEquals("", resolved);
    }

    /**
     * 通过反射调用私有方法，验证 cat 扫描窗口计算结果。
     *
     * @param requestedLines 请求展示行数
     * @param filterRules    过滤规则
     * @return 实际扫描行数
     * @throws Exception 反射调用异常
     */
    private int invokeResolveCatScanLines(int requestedLines, List<LogStreamRequest.FilterRule> filterRules)
            throws Exception {
        Method method = SshLogService.class
                .getDeclaredMethod("resolveCatScanLines", int.class, List.class);
        method.setAccessible(true);
        return (Integer) method.invoke(sshLogService, requestedLines, filterRules);
    }

    /**
     * 通过反射调用“是否包含 include 规则”判定方法。
     *
     * @param filterRules 规则集合
     * @return true 表示包含 include 规则
     * @throws Exception 反射调用异常
     */
    private boolean invokeHasIncludeFilterRule(List<LogStreamRequest.FilterRule> filterRules) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod("hasIncludeFilterRule", List.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(sshLogService, filterRules);
    }

    /**
     * 通过反射调用默认文件选择方法。
     *
     * @param candidates 候选文件路径
     * @return 选中的默认路径
     * @throws Exception 反射调用异常
     */
    private String invokeResolvePreferredLogFilePath(List<String> candidates) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod("resolvePreferredLogFilePath", List.class);
        method.setAccessible(true);
        return (String) method.invoke(sshLogService, candidates);
    }

    /**
     * 构建过滤规则对象。
     *
     * @param type 规则类型
     * @param data 规则内容
     * @return 过滤规则
     */
    private LogStreamRequest.FilterRule rule(String type, String data) {
        LogStreamRequest.FilterRule filterRule = new LogStreamRequest.FilterRule();
        filterRule.setType(type);
        filterRule.setData(data);
        return filterRule;
    }
}
