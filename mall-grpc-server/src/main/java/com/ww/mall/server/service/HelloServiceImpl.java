package com.ww.mall.server.service;

import cn.hutool.core.util.RandomUtil;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.ww.mall.common.enums.SensitiveWordHandlerType;
import com.ww.mall.proto.hello.HelloRequest;
import com.ww.mall.proto.hello.HelloResponse;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import com.ww.mall.sensitive.annotation.MallSensitiveWordHandler;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-12 9:35
 * @description:
 */
@Slf4j
@GrpcService
public class HelloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {

    @Resource
    private SensitiveWordBs sensitiveWordBs;

    /**
     * 普通prc
     *
     * @param request client请求参数
     * @param responseObserver server返回client响应
     */
    @Override
    @MallSensitiveWordHandler(content = {"#request.name", "#request.name"}, handlerType = SensitiveWordHandlerType.REPLACE)
    public void hello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        // 接受客户端请求参数
        String content = request.getName();
        log.info("用户端输入的内容：{}", content);
        // 封装响应
        HelloResponse.Builder responseBuilder = HelloResponse.newBuilder();
        responseBuilder.setResult(content);
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
        log.info("服务端业务处理");
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

