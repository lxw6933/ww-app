package com.ww.mall.netty.run;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-07 22:58
 * @description:
 */
@Slf4j
@Component
public class ApplicationRunner implements CommandLineRunner {

    @Resource
    private ServerBootstrap chatServerBootstrap;

    @Override
    public void run(String... args) throws Exception {
//        Channel channel = chatServerBootstrap.bind(8765).sync().channel();
//        log.info("聊天服务监听完成....");
//        channel.closeFuture().addListener((ChannelFutureListener) future -> {
//            // 在 Channel 关闭时执行特定的操作，比如释放资源等
//            log.info("监听netty server close");
//        });
    }
}
