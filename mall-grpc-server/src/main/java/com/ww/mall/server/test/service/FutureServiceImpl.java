package com.ww.mall.server.test.service;

import com.ww.mall.proto.hello.FutureProto;
import com.ww.mall.proto.hello.FutureServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-05-11 23:31
 * @description:
 */
@Slf4j
public class FutureServiceImpl extends FutureServiceGrpc.FutureServiceImplBase {

    @Override
    public void test(FutureProto.FutureRequest request, StreamObserver<FutureProto.FutureResponse> responseObserver) {
        // 接受客户端请求参数
        String name = request.getName();
        // 业务处理
        log.info("服务端业务处理");
        // 封装响应
        FutureProto.FutureResponse.Builder responseBuilder = FutureProto.FutureResponse.newBuilder();
        responseBuilder.setResult("success");
        // 将响应消息通过网络回传给client
        responseObserver.onNext(responseBuilder.build());
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }

}
