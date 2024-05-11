package com.ww.mall.netty.grpc.client;

import com.ww.mall.proto.hello.HelloProto;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-05-11 0:10
 * @description: 【非阻塞客户端】
 */
@Slf4j
public class HelloGrpcSubClient {

    public static void main(String[] args) {
        ManagedChannel managedChannel = null;
        try {
            // 创建通信管道
            managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 6933).usePlaintext().build();
            // 获取代理对象【非阻塞对象，处理服务端流式消息】
            HelloServiceGrpc.HelloServiceStub stubHelloService = HelloServiceGrpc.newStub(managedChannel);
            // 请求参数
            HelloProto.HelloRequest.Builder requestBuilder = HelloProto.HelloRequest.newBuilder();
            requestBuilder.setName("hello");
            HelloProto.HelloRequest request = requestBuilder.build();
            // 1.rpc远程调用
            stubHelloService.serverStreamHello(request, new StreamObserver<HelloProto.HelloResponse>() {
                @Override
                public void onNext(HelloProto.HelloResponse helloResponse) {
                    log.info("rpc远程调用，请求参数：{} 响应数据：{}", request, helloResponse);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.info("rpc远程调用，请求参数：{} 异常响应数据：{}", request, throwable.getCause().getMessage());
                }

                @Override
                public void onCompleted() {
                    log.info("rpc远程调用，请求参数：{} 响应完成", request);
                }
            });
            managedChannel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("rpc通信异常", e);
        } finally {
            if (managedChannel != null) {
                managedChannel.shutdown();
            }
        }
    }

}
