package com.ww.mall.netty.grpc.client;

import com.ww.mall.proto.hello.HelloProto;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-05-11 0:10
 * @description: 【非阻塞客户端】
 */
@Slf4j
public class HelloGrpcStreamClient {

    public static void main(String[] args) {
        ManagedChannel managedChannel = null;
        try {
            // 创建通信管道
            managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 6933).usePlaintext().build();
            // 获取代理对象【非阻塞对象，处理服务端流式消息】
            HelloServiceGrpc.HelloServiceStub streamClientHelloService = HelloServiceGrpc.newStub(managedChannel);
            // 1.rpc远程调用
            StreamObserver<HelloProto.HelloRequest> helloRequestStreamObserver = streamClientHelloService.clientStreamHello(new StreamObserver<HelloProto.HelloResponse>() {
                // 监听每次发给服务端消息后 收到服务端的消息回调响应
                @Override
                public void onNext(HelloProto.HelloResponse helloResponse) {
                    log.info("收到服务端响应：{}", helloResponse);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.info("收到服务端异常：{}", throwable.getCause().getMessage());
                }

                @Override
                public void onCompleted() {
                    log.info("收到服务端完成最终操作响应");
                }
            });

            for (int i = 0; i < 10; i++) {
                HelloProto.HelloRequest.Builder builder = HelloProto.HelloRequest.newBuilder();
                builder.setName("client stream消息" + i);
                HelloProto.HelloRequest request = builder.build();
                helloRequestStreamObserver.onNext(request);
            }
            // 所有消息发送完毕
            helloRequestStreamObserver.onCompleted();
        } catch (Exception e) {
            log.error("rpc通信异常", e);
        } finally {
            if (managedChannel != null) {
                managedChannel.shutdown();
            }
        }
    }

}
