package com.ww.mall.client.controller;

import com.ww.mall.proto.hello.HelloRequest;
import com.ww.mall.proto.hello.HelloResponse;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
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
    private MallPublisher mallPublisher;

    @RequestMapping("/test")
    public String test(@RequestParam("msg") Integer msg) {
        mallPublisher.publishMsg(ExchangeConstant.MALL_COUPON_EXCHANGE, RouteKeyConstant.MALL_COUPON_TEST_KEY, msg);
        return "success";
    }

}
