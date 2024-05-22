package com.ww.mall.web.interceptor.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-22 23:27
 * @description:
 */
@Slf4j
@Component
public class GrpcMonitorInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        long startTime = System.nanoTime();
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        long endTime = System.nanoTime();
                        long elapsedTime = endTime - startTime;
                        log.info("Request {} took {} nanoseconds", method.getFullMethodName(), elapsedTime);
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }

}
