package com.ww.mall.web.utils;

import com.alibaba.cloud.sentinel.rest.SentinelClientHttpResponse;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

/**
 * @author ww
 * @create 2024-04-28- 10:34
 * @description:
 */
@Slf4j
public class RestTemplateExceptionUtil {

    // 服务流量控制处理
    public static ClientHttpResponse handleException(HttpRequest request, byte[] body, ClientHttpRequestExecution execution, BlockException exception) {
        log.error("restTemplate 远程调用第三方接口【{}】响应数据【{}】异常[handleException blockException]: {}", request.getURI(), new String(body), exception.getMessage());
        return new SentinelClientHttpResponse("{\"code\":\"500\",\"msg\": \"服务流量控制处理\"}");
    }

    // 服务熔断降级处理
    public static ClientHttpResponse fallback(HttpRequest request, byte[] body, ClientHttpRequestExecution execution, BlockException exception) {
        log.error("restTemplate 远程调用第三方接口【{}】响应数据【{}】异常[fallback blockException]: {}", request.getURI(), new String(body), exception.getMessage());
        return new SentinelClientHttpResponse("{\"code\":\"500\",\"msg\": \"服务熔断降级处理\"}");
    }

}
