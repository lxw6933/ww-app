package com.ww.mall.seckill.grpc;

import com.ww.mall.proto.hello.HelloRequest;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-22- 18:24
 * @description:
 */
@Service
public class GrpcClientService {

    private final HelloServiceGrpc.HelloServiceBlockingStub helloServiceBlockingStub;

    @Autowired
    public GrpcClientService(ManagedChannel mallMemberChannel) {
        helloServiceBlockingStub = HelloServiceGrpc.newBlockingStub(mallMemberChannel);
    }

    public String sendMessage(String name) {
        // 调用 gRPC 服务
        return helloServiceBlockingStub.hello(HelloRequest.newBuilder().setName(name).build()).getResult();
    }

}
