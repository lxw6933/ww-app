package com.ww.mall.web.interceptor.grpc.client;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-22 23:23
 * @description:
 */
@Slf4j
@Component
public class GrpcRetryInterceptor implements ClientInterceptor {

    private static final int MAX_RETRIES = 3;

    private static final long RETRY_INTERVAL_MS = 1000;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            int retries = 0;
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // 添加异常处理逻辑
                ClientCall<ReqT, RespT> delegate = this;
                Listener<RespT> retryingResponseListener = new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                        responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (status.getCode() == Status.Code.UNAVAILABLE && retries < MAX_RETRIES) {
                            log.info("start retry {} status: {}", retries, status);
                            retries++;
                            try {
                                Thread.sleep(RETRY_INTERVAL_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                responseListener.onClose(status, trailers);
                                return;
                            }
                            delegate.start(this, headers);
                            return;
                        }
                        super.onClose(status, trailers);
                    }
                };
                delegate.start(retryingResponseListener, headers);
            }
        };
    }

}
