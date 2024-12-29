package com.ww.mall.seckill.grpc;

import com.ww.mall.proto.hello.HelloRequest;
import com.ww.mall.proto.hello.HelloServiceGrpc;
import io.grpc.ClientInterceptor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-05-22- 18:24
 * @description:
 */
@Service
public class GrpcClientService {

    @GrpcClient("ww-member")
    private HelloServiceGrpc.HelloServiceBlockingStub helloServiceBlockingStub;

    @Resource
    private ClientInterceptor globalClientInterceptor;

    public String sendMessage(String name) {
        // 调用 gRPC 服务
        return helloServiceBlockingStub
                // 配置请求超时时间
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                // 配置拦截器
                .withInterceptors(globalClientInterceptor)
                .hello(HelloRequest.newBuilder().setName(name).build())
                .getResult();
    }

}
