package com.ww.mall.netty.service;

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
     * 接受client请求参数
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
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
