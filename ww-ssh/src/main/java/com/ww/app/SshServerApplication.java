package com.ww.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ww-ssh 模块启动入口。
 * <p>
 * 模块职责：
 * 1. 提供环境/服务维度的日志配置查询接口；
 * 2. 提供远程日志文件发现能力；
 * 3. 通过 WebSocket 实时推送目标服务器日志内容。
 * </p>
 */
@SpringBootApplication
public class SshServerApplication {

    /**
     * 应用启动主方法。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SshServerApplication.class, args);
    }
}
