package com.ww.mall.quartz.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * @Description:
 * @Author: ww
 * @CreateDate: 2021/4/6 下午11:51
 **/
public class SampleJob extends QuartzJobBean {

    private String name;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("执行任务！");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
