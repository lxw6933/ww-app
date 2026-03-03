package com.ww.app.ssh.service.support;

import com.ww.app.ssh.model.LogStreamRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * {@link LogLineFilterMatcher} 过滤规则测试。
 */
class LogLineFilterMatcherTest {

    /**
     * 包含关键字为空时不限制展示。
     */
    @Test
    void shouldPassWhenIncludeKeywordEmpty() {
        LogLineFilterMatcher matcher = new LogLineFilterMatcher();
        Assertions.assertTrue(matcher.matches("INFO 启动完成", "", ""));
    }

    /**
     * 包含关键字存在但不命中时应过滤。
     */
    @Test
    void shouldRejectWhenIncludeNotMatch() {
        LogLineFilterMatcher matcher = new LogLineFilterMatcher();
        Assertions.assertFalse(matcher.matches("INFO 启动完成", "ERROR", ""));
    }

    /**
     * 排除关键字命中时应过滤。
     */
    @Test
    void shouldRejectWhenExcludeMatch() {
        LogLineFilterMatcher matcher = new LogLineFilterMatcher();
        Assertions.assertFalse(matcher.matches("DEBUG trace", "", "DEBUG"));
    }

    /**
     * 同时设置包含和排除时，排除规则优先。
     */
    @Test
    void shouldRejectWhenBothMatchButExcludeHit() {
        LogLineFilterMatcher matcher = new LogLineFilterMatcher();
        Assertions.assertFalse(matcher.matches("ERROR with DEBUG details", "ERROR", "DEBUG"));
    }

    /**
     * 校验链式规则：多个包含条件为 AND。
     */
    @Test
    void shouldApplyIncludeRulesAsAnd() {
        LogLineFilterMatcher matcher = new LogLineFilterMatcher();
        LogStreamRequest.FilterRule includeError = rule(LogStreamRequest.FILTER_TYPE_INCLUDE, "ERROR");
        LogStreamRequest.FilterRule includeOrder = rule(LogStreamRequest.FILTER_TYPE_INCLUDE, "orderId");
        Assertions.assertTrue(matcher.matches("ERROR orderId=1", Arrays.asList(includeError, includeOrder)));
        Assertions.assertFalse(matcher.matches("ERROR only", Arrays.asList(includeError, includeOrder)));
    }

    /**
     * 校验链式规则：排除命中任一即过滤。
     */
    @Test
    void shouldRejectWhenAnyExcludeRuleMatched() {
        LogLineFilterMatcher matcher = new LogLineFilterMatcher();
        LogStreamRequest.FilterRule includeError = rule(LogStreamRequest.FILTER_TYPE_INCLUDE, "ERROR");
        LogStreamRequest.FilterRule excludeDebug = rule(LogStreamRequest.FILTER_TYPE_EXCLUDE, "DEBUG");
        Assertions.assertFalse(matcher.matches("ERROR DEBUG details", Arrays.asList(includeError, excludeDebug)));
    }

    /**
     * 创建过滤规则对象。
     *
     * @param type 规则类型
     * @param data 规则数据
     * @return 规则对象
     */
    private LogStreamRequest.FilterRule rule(String type, String data) {
        LogStreamRequest.FilterRule rule = new LogStreamRequest.FilterRule();
        rule.setType(type);
        rule.setData(data);
        return rule;
    }
}
