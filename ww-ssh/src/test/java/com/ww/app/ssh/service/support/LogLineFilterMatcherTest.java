package com.ww.app.ssh.service.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
