package com.ww.app.ssh.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
