package com.ww.app.client.test;

import com.ww.app.proto.hello.HelloRequest;
import com.ww.app.proto.hello.HelloResponse;
import com.ww.app.proto.hello.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-05-11 0:10
 * @description: 阻塞客户端【远程调用】
 */
@Slf4j
public class HelloGrpcBlockClient {

    public static void main(String[] args) {
        ManagedChannel managedChannel = null;
        try {
            // 创建通信管道
            managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 6933).usePlaintext().build();
            // 获取代理对象【阻塞代理对象】
            HelloServiceGrpc.HelloServiceBlockingStub blockingStubHelloService = HelloServiceGrpc.newBlockingStub(managedChannel);
            // 请求参数
            HelloRequest.Builder requestBuilder = HelloRequest.newBuilder();
            requestBuilder.setName("hello");
            HelloRequest request = requestBuilder.build();
            // 1.rpc远程调用
            HelloResponse response = blockingStubHelloService.hello(request);
            log.info("rpc远程调用，请求参数：{} 响应参数：{}", request, response);
        } catch (Exception e) {
            log.error("rpc通信异常", e);
        } finally {
            if (managedChannel != null) {
                managedChannel.shutdown();
            }
        }
    }

}
