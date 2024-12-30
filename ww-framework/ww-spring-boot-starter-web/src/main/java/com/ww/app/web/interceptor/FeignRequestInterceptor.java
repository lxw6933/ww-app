package com.ww.app.web.interceptor;

import com.ww.app.common.constant.Constant;
import com.ww.app.common.thread.ThreadMdcUtil;
import com.ww.app.web.holder.ServerIpContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @description: feign远程调用拦截器
 * @author: ww
 * @create: 2023/7/16 18:01
 **/
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        // feign异步多线程多次调用存在null，需要每个线程set主线程的attributes
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    String values = request.getHeader(name);
                    // 跳过 content-length
                    if ("content-length".equals(name)) {
                        continue;
                    }
                    requestTemplate.header(name, values);
                }
            }
        }
        // 讲当前服务traceId传递到远程调用的服务
        String traceId = ThreadMdcUtil.getTraceId();
        requestTemplate.header(Constant.TRACE_ID, traceId);
        requestTemplate.header(Constant.FEIGN_FLAG, "true");
        // 远程调用是否指定服务
        String serverIp = ServerIpContextHolder.get();
        if (StringUtils.isNotBlank(serverIp)) {
            requestTemplate.header(Constant.SERVER_IP, serverIp);
        }
    }
}
