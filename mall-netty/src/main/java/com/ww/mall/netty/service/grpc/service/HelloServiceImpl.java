package com.ww.mall.netty.service.grpc.service;

import cn.hutool.core.util.RandomUtil;
import com.ww.mall.proto.hello.HelloProto;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-05-11 0:01
 * @description:
 */
@Slf4j
public class HelloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {

    /**
     * 普通prc
     *
     * @param request client请求参数
     * @param responseObserver server返回client响应
     */
    @Override
    public void hello(HelloProto.HelloRequest request, StreamObserver<HelloProto.HelloResponse> responseObserver) {
        // 接受客户端请求参数
        String name = request.getName();
        // 业务处理
        log.info("服务端业务处理");
        // 封装响应
        HelloProto.HelloResponse.Builder responseBuilder = HelloProto.HelloResponse.newBuilder();
        responseBuilder.setResult("success");
        // 将响应消息通过网络回传给client
        responseObserver.onNext(responseBuilder.build());
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }

    /**
     * 服务端流rpc
     *
     * @param request client请求参数
     * @param responseObserver server返回client响应
     */
    @Override
    public void serverStreamHello(HelloProto.HelloRequest request, StreamObserver<HelloProto.HelloResponse> responseObserver) {
        // 接受客户端请求参数
        String name = request.getName();
        // 业务处理
        log.info("服务端业务处理");
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(RandomUtil.randomInt(1, 5) * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 封装响应
            HelloProto.HelloResponse.Builder responseBuilder = HelloProto.HelloResponse.newBuilder();
            responseBuilder.setResult("success");
            // 将响应消息通过网络回传给client
            responseObserver.onNext(responseBuilder.build());
        }
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }

    /**
     * 客户端流rpc
     *
     * @param responseObserver response监听者
     * @return request监听者
     */
    @Override
    public StreamObserver<HelloProto.HelloRequest> clientStreamHello(StreamObserver<HelloProto.HelloResponse> responseObserver) {
        return new StreamObserver<HelloProto.HelloRequest>() {
            @Override
            public void onNext(HelloProto.HelloRequest helloRequest) {
                log.info("接收到client发送的消息：{}", helloRequest);
                HelloProto.HelloResponse.Builder builder = HelloProto.HelloResponse.newBuilder();
                builder.setResult("收到消息：" + helloRequest.getName());
                HelloProto.HelloResponse response = builder.build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                log.info("监听到客户端所有消息发送完毕");
                HelloProto.HelloResponse.Builder builder = HelloProto.HelloResponse.newBuilder();
                builder.setResult("收到所有消息，处理完毕");
                HelloProto.HelloResponse response = builder.build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * 双向流rpc
     *
     * @param responseObserver response监听者
     * @return request监听者
     */
    @Override
    public StreamObserver<HelloProto.HelloRequest> duplexStreamHello(StreamObserver<HelloProto.HelloResponse> responseObserver) {
        return new StreamObserver<HelloProto.HelloRequest>() {
            @Override
            public void onNext(HelloProto.HelloRequest helloRequest) {
                log.info("接收到client发来的消息：{}", helloRequest);
                responseObserver.onNext(HelloProto.HelloResponse.newBuilder().setResult("123").build());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                log.info("接收到client发来completed消息");
                responseObserver.onCompleted();
            }

        };
    }
}
