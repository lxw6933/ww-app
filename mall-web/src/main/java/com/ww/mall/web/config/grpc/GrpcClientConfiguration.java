package com.ww.mall.web.config.grpc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-05-22- 18:19
 * @description:
 */
@Configuration
public class GrpcClientConfiguration {

    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        map.put("1", 100);
        map.put("2", 20);
        System.out.println(map.values());
        System.out.println(map.values().stream().reduce(Integer::sum).orElse(0));
    }

    @Bean
    public ManagedChannel mallMemberChannel() {
        // 创建 gRPC 通道，连接到 gRPC 服务端的多个实例
        return ManagedChannelBuilder.forTarget("mall-member")
                // 设置负载均衡策略
                .defaultLoadBalancingPolicy("round_robin")
                // 在本示例中，我们使用的是明文通信，生产环境中应使用安全连接
                .usePlaintext()
                .build();
    }

//    @Bean
//    public ManagedChannel mallThirdServerChannel() {
//        // 创建 gRPC 通道，连接到 gRPC 服务端的多个实例
//        return ManagedChannelBuilder.forTarget("mall-third-server")
//                // 设置负载均衡策略
//                .defaultLoadBalancingPolicy("round_robin")
//                // 在本示例中，我们使用的是明文通信，生产环境中应使用安全连接
//                .usePlaintext()
//                .build();
//    }

}
