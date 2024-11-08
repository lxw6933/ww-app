package com.ww.mall.server.test;

import com.ww.mall.server.test.service.FutureServiceImpl;
import com.ww.mall.server.test.service.HelloServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * @author ww
 * @create 2024-05-11 0:06
 * @description: rpc
 */
public class HelloGrpcServer {

    public static void main(String[] args) throws Exception {
        // 绑定端口
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(6933);
        // 发布服务
        serverBuilder.addService(new HelloServiceImpl());
        serverBuilder.addService(new FutureServiceImpl());
        // 创建服务对象
        Server server = serverBuilder.build();
        server.start();
        server.awaitTermination();
    }

}
