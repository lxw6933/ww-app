package com.ww.mall.client.controller;

import com.ww.mall.proto.hello.HelloProto;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ww
 * @create 2024-05-12 9:45
 * @description:
 */
@RestController
public class ClientController {

    @GrpcClient("grpc-server")
    private HelloServiceGrpc.HelloServiceBlockingStub helloServiceBlockingStub;

    @RequestMapping("/hello")
    public String hello() {
        HelloProto.HelloResponse helloResponse = helloServiceBlockingStub.hello(HelloProto.HelloRequest.newBuilder().setName("wwwww").build());
        return helloResponse.getResult();
    }

}
