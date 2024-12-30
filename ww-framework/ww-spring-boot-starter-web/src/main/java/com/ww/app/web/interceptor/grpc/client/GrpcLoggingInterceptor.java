package com.ww.app.web.interceptor.grpc.client;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-22 23:16
 * @description:
 */
@Slf4j
@Component
public class GrpcLoggingInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        log.info("Sending request: {}", method);
        return next.newCall(method, callOptions);
    }
}
