package com.ww.mall.web.interceptor.grpc;

import com.ww.mall.common.exception.ApiException;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-05-22 23:29
 * @description:
 */
@Slf4j
@Component
public class RequestRateInterceptor implements ClientInterceptor {

    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private static final int MAX_REQUEST_SIZE = 1024 * 1024;

    private final Semaphore requestSemaphore = new Semaphore(MAX_REQUESTS_PER_SECOND);
    private final Semaphore requestSizeSemaphore = new Semaphore(MAX_REQUEST_SIZE);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        // 请求限流
        try {
            if (!requestSemaphore.tryAcquire(1, 1, TimeUnit.SECONDS)) {
                throw new ApiException("Request rate limit exceeded");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Thread interrupted", e);
        }

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // 请求大小限流
                int requestSize = getRequestSize(headers);
                if (requestSize > MAX_REQUEST_SIZE || !requestSizeSemaphore.tryAcquire(requestSize)) {
                    throw new RuntimeException("Request size limit exceeded");
                }

                super.start(responseListener, headers);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                // 释放请求大小的限流
                int requestSize = getRequestSize(trailers);
                requestSizeSemaphore.release(requestSize);
                super.close(status, trailers);
            }
        };
    }

    private int getRequestSize(Metadata metadata) {
        // 在实际应用中根据需要获取请求大小，这里假设请求大小为固定值
        return 1024;
    }

}
