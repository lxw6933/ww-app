package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.ConcurrentStreamAccessResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * {@link ConcurrentStreamUsageController} 单元测试。
 */
class ConcurrentStreamUsageControllerTest {

    /**
     * 校验通过回环地址访问页面时，允许展示并发流入口按钮。
     */
    @Test
    void shouldEnableAccessWhenVisitedByLoopbackIp() {
        ConcurrentStreamUsageController controller = new ConcurrentStreamUsageController(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("127.0.0.1");

        ConcurrentStreamAccessResponse response = controller.access(request);

        Assertions.assertTrue(response.isEnabled());
    }

    /**
     * 校验通过 localhost 访问页面时，允许展示并发流入口按钮。
     */
    @Test
    void shouldEnableAccessWhenVisitedByLocalhost() {
        ConcurrentStreamUsageController controller = new ConcurrentStreamUsageController(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");

        ConcurrentStreamAccessResponse response = controller.access(request);

        Assertions.assertTrue(response.isEnabled());
    }

    /**
     * 校验通过非部署机 IP 的域名访问页面时，不展示并发流入口按钮。
     */
    @Test
    void shouldDisableAccessWhenVisitedByDomainHost() {
        ConcurrentStreamUsageController controller = new ConcurrentStreamUsageController(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("log.example.com");

        ConcurrentStreamAccessResponse response = controller.access(request);

        Assertions.assertFalse(response.isEnabled());
    }
}
