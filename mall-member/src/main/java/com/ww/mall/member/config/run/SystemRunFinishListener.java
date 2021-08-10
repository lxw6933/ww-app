package com.ww.mall.member.config.run;

import com.ww.mall.member.config.redis.RedisManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringJoiner;

/**
 * @description: 系统启动完成
 * @author: ww
 * @create: 2021-05-12 18:39
 */
@Slf4j
@Component
public class SystemRunFinishListener implements ApplicationListener<ApplicationStartedEvent> {

    @Resource
    private RedisManager redisManager;

    @Resource
    private ConfigurableApplicationContext context;

    @Value("${server.port}")
    private String port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${spring.profiles.active}")
    private String active;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private String redisPost;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            redisManager.hasKey("local:test:connect");
        } catch (Exception e) {
            log.error(" ____   __    _   _ ");
            log.error("| |_   / /\\  | | | |");
            log.error("|_|   /_/--\\ |_| |_|__");
            log.error("                        ");
            log.error("Redis连接异常，{}", e.getMessage());
            log.error("请检查Redis连接配置并确保Redis服务已启动");
            // 关闭 项目
            context.close();
        }
        if (context.isActive()) {
            InetAddress address;
            try {
                address = InetAddress.getLocalHost();
                String url = String.format("http://%s:%s", address.getHostAddress(), port);
                if (StringUtils.isNotBlank(contextPath)) {
                    url += contextPath;
                }
                log.info(" __    ___   _      ___   _     ____ _____  ____ ");
                log.info("/ /`  / / \\ | |\\/| | |_) | |   | |_   | |  | |_  ");
                log.info("\\_\\_, \\_\\_/ |_|  | |_|   |_|__ |_|__  |_|  |_|__ ");
                log.info("                                                      ");
                log.info("项目系统启动完毕，后台环境：{}，后台地址：{}", active, url);
                log.info("redis服务启动成功：{}", new StringJoiner(":").add(redisHost).add(redisPost));
                String os = System.getProperty("os.name");
                // 默认为 windows时才自动打开页面
                if (StringUtils.containsIgnoreCase(os, "windows")) {
                    //使用默认浏览器打开系统登录页  Runtime.getRuntime().exec("cmd  /c  start " + url);
                    log.info("项目部署环境是：【Windows】");
                } else {
                    log.info("项目部署环境是：【Linux】");
                }
            } catch (UnknownHostException e) {
                log.error("获取host地址异常：{}", e.getMessage());
            }
        }
    }
}
