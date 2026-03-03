package com.ww.app.ssh.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * {@link LogStreamRequest} 行数与聚合标识测试。
 */
class LogStreamRequestTest {

    /**
     * 校验回看行数边界归一化逻辑。
     */
    @Test
    void shouldNormalizeLinesWithinRange() {
        LogStreamRequest request = new LogStreamRequest();

        request.setLines(1);
        Assertions.assertEquals(10, request.normalizedLines());

        request.setLines(99999);
        Assertions.assertEquals(5000, request.normalizedLines());

        request.setLines(200);
        Assertions.assertEquals(200, request.normalizedLines());

        request.setLines(null);
        Assertions.assertEquals(200, request.normalizedLines());
    }

    /**
     * 校验 ALL 占位值判断逻辑。
     */
    @Test
    void shouldRecognizeAllScope() {
        LogStreamRequest request = new LogStreamRequest();
        request.setEnv("__ALL__");
        request.setService("__all__");
        Assertions.assertTrue(request.isAllEnv());
        Assertions.assertTrue(request.isAllService());
    }

    /**
     * 校验链式过滤规则规范化。
     */
    @Test
    void shouldNormalizeFilterRules() {
        LogStreamRequest.FilterRule include = new LogStreamRequest.FilterRule();
        include.setType("包含");
        include.setData(" ERROR ");

        LogStreamRequest.FilterRule exclude = new LogStreamRequest.FilterRule();
        exclude.setType("exclude");
        exclude.setData("DEBUG");

        LogStreamRequest.FilterRule invalid = new LogStreamRequest.FilterRule();
        invalid.setType("unknown");
        invalid.setData("xxx");

        LogStreamRequest request = new LogStreamRequest();
        request.setFilterRules(Arrays.asList(include, exclude, invalid));

        List<LogStreamRequest.FilterRule> rules = request.normalizedFilterRules();
        Assertions.assertEquals(2, rules.size());
        Assertions.assertEquals(LogStreamRequest.FILTER_TYPE_INCLUDE, rules.get(0).getType());
        Assertions.assertEquals("ERROR", rules.get(0).getData());
        Assertions.assertEquals(LogStreamRequest.FILTER_TYPE_EXCLUDE, rules.get(1).getType());
    }
}
