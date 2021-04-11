package com.ww.mall.quartz.run;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Description: spring容器初始化完成执行初始化定时任务
 * @Author: ww
 * @CreateDate: 2021/4/5 下午11:11
 **/
@Component
@Order(value = 1)
public class JobScheduleRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==============开始初始化定时任务==============");
        System.out.println("==============初始化定时任务结束==============");
    }
}
