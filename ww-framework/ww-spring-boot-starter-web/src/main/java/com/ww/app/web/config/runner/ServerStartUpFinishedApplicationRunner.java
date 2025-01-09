package com.ww.app.web.config.runner;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.utils.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-01-02- 17:17
 * @description: 项目启动完成
 */
@Slf4j
public class ServerStartUpFinishedApplicationRunner implements ApplicationRunner {

    @Value("${spring.application.name}")
    private String serverName;

    @Value("${server.port}")
    private String serverPort;

    @Value("${server.servlet.context-path}")
    private String serverContextPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ThreadUtil.execute(() -> {
            String ip = IpUtil.getLocalIp();
            String serverReqPath = "http://" + ip + StrUtil.COLON + serverPort + serverContextPath + StrUtil.SLASH;
            // 延迟 1 秒，保证输出到结尾
            ThreadUtil.sleep(1, TimeUnit.SECONDS);
            log.info("\n----------------------------------------------------------\n\t" +
                            "项目:\t[ww-app]-[{}]服务模块,启动成功！\n\t" +
                            "接口路径:\t{} \n\t" +
                            "接口文档:\t{} \n\t" +
                            "prometheus metrics:\t{} \n" +
                            "----------------------------------------------------------",
                    serverName,
                    serverReqPath,
                    serverReqPath + "doc.html",
                    serverReqPath + "actuator/prometheus");
        });
    }
}
