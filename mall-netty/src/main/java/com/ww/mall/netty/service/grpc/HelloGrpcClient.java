package com.ww.mall.netty.service.grpc;

import com.ww.mall.proto.hello.HelloProto;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-05-11 0:10
 * @description:
 */
@Slf4j
public class HelloGrpcClient {

    public static void main(String[] args) {
        ManagedChannel managedChannel = null;
        try {
            // 创建通信管道
            managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 6933).usePlaintext().build();
            // 获取代理对象
            HelloServiceGrpc.HelloServiceBlockingStub blockingStubHelloService = HelloServiceGrpc.newBlockingStub(managedChannel);
            // 请求参数
            HelloProto.HelloRequest.Builder requestBuilder = HelloProto.HelloRequest.newBuilder();
            requestBuilder.setName("hello");
            HelloProto.HelloRequest request = requestBuilder.build();
            // rpc远程调用
            HelloProto.HelloResponse response = blockingStubHelloService.hello(request);
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
