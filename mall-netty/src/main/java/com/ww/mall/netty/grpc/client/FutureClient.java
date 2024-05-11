package com.ww.mall.netty.grpc.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.proto.hello.FutureProto;
import com.ww.mall.proto.hello.FutureServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;

/**
 * @author ww
 * @create 2024-05-11 23:34
 * @description:
 */
@Slf4j
public class FutureClient {

    public static void main(String[] args) {
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 9998).build();
        try {
            FutureServiceGrpc.FutureServiceFutureStub futureService = FutureServiceGrpc.newFutureStub(managedChannel);
            ListenableFuture<FutureProto.FutureResponse> futureMessageResponse = futureService.test(FutureProto.FutureRequest.newBuilder().setName("future message request").build());

            // 同步操作
            FutureProto.FutureResponse futureResponse = futureMessageResponse.get();

            // 异步操作
//            futureMessageResponse.addListener(() -> log.info("异步rpc响应"), Executors.newCachedThreadPool());
            Futures.addCallback(futureMessageResponse, new FutureCallback<FutureProto.FutureResponse>() {
                @Override
                public void onSuccess(FutureProto.FutureResponse result) {
                    log.info("异步rpc响应成功：{}", result);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.info("异步rpc响应异常", t);
                }
            }, Executors.newCachedThreadPool());

            log.info("future end");
        } catch (Exception e) {
            log.error("future client exception", e);
            throw new ApiException("future client exception");
        } finally {
            managedChannel.shutdown();
        }
    }

}
