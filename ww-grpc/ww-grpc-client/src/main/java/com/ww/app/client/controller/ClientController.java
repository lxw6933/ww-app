package com.ww.app.client.controller;

import com.ww.app.proto.hello.HelloRequest;
import com.ww.app.proto.hello.HelloResponse;
import com.ww.app.proto.hello.HelloServiceGrpc;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.rabbitmq.exchange.ExchangeConstant;
import com.ww.app.rabbitmq.routekey.RouteKeyConstant;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

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
    public String hello(@RequestParam("content") String content) {
        HelloResponse helloResponse = helloServiceBlockingStub.hello(HelloRequest.newBuilder().setName(content).build());
        return helloResponse.getResult();
    }

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @RequestMapping("/test")
    public String test(@RequestParam("msg") Integer msg) {
        rabbitMqPublisher.publishMsg(ExchangeConstant.COUPON_EXCHANGE, RouteKeyConstant.COUPON_TEST_KEY, msg);
        return "success";
    }

}
