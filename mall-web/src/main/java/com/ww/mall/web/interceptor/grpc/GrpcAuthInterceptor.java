package com.ww.mall.web.interceptor.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-22 23:18
 * @description:
 */
@Slf4j
@Component
public class GrpcAuthInterceptor implements ClientInterceptor {

    private final String authToken;

    public GrpcAuthInterceptor(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return next.newCall(method, callOptions.withAuthority(authToken));
    }
}

