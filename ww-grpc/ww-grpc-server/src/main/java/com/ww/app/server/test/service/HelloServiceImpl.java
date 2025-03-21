package com.ww.app.server.test.service;

import cn.hutool.core.util.RandomUtil;
import com.ww.app.proto.hello.HelloRequest;
import com.ww.app.proto.hello.HelloResponse;
import com.ww.app.proto.hello.HelloServiceGrpc;
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
    public void hello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        // 接受客户端请求参数
        String name = request.getName();
        // 业务处理
        log.info("服务端业务处理客户端{}业务", name);
        // 封装响应
        HelloResponse.Builder responseBuilder = HelloResponse.newBuilder();
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
    public void serverStreamHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        // 接受客户端请求参数
        String name = request.getName();
        // 业务处理
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(RandomUtil.randomInt(1, 5) * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 封装响应
            HelloResponse.Builder responseBuilder = HelloResponse.newBuilder();
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
    public StreamObserver<HelloRequest> clientStreamHello(StreamObserver<HelloResponse> responseObserver) {
        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest helloRequest) {
                log.info("接收到client发送的消息：{}", helloRequest);
                HelloResponse.Builder builder = HelloResponse.newBuilder();
                builder.setResult("收到消息：" + helloRequest.getName());
                HelloResponse response = builder.build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                log.info("监听到客户端所有消息发送完毕");
                HelloResponse.Builder builder = HelloResponse.newBuilder();
                builder.setResult("收到所有消息，处理完毕");
                HelloResponse response = builder.build();
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
    public StreamObserver<HelloRequest> duplexStreamHello(StreamObserver<HelloResponse> responseObserver) {
        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest helloRequest) {
                log.info("接收到client发来的消息：{}", helloRequest);
                responseObserver.onNext(HelloResponse.newBuilder().setResult("123").build());
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
