package com.ww.app.web.interceptor.grpc.client;

import com.ww.app.common.exception.ApiException;
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
            throw new ApiException("Thread interrupted:" + e.getMessage());
        }
        return null;
    }

}
