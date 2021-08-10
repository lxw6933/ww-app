package com.ww.mall.quartz.listener;

import com.ww.mall.common.utils.JsonUtils;
import com.ww.mall.mvc.entity.SysJobDetailEntity;
import com.ww.mall.mvc.entity.SysJobDetailLogEntity;
import com.ww.mall.mvc.service.SysJobDetailLogService;
import com.ww.mall.mvc.service.SysJobDetailService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @description: 全局定时任务监听器
 * @author: ww
 * @create: 2021-05-13 19:44
 */
@Slf4j
@Component
public class GlobalJobListener implements JobListener {

    @Resource
    private SysJobDetailLogService sysJobDetailLogService;

    @Resource
    private SysJobDetailService sysJobDetailService;

    /** 线程变量 */
    private final ThreadLocal<SysJobDetailLogEntity> logTheadLocal = new ThreadLocal<>();

    /**
     * JobListener 标识名称，用于事件广播
     * @return String
     */
    @Override
    public String getName() {
        return "global-job-listener";
    }

    /**
     * 由调度程序调用时JobDetail即将被执行(一个关联触发发生)。
     * @param jobExecutionContext 任务执行上下文
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {
        log.info("任务监听器触发： jobToBeExecuted ----- 任务信息：【 任务名：[{}]、任务组：[{}]、任务类：[{}]】",
                jobExecutionContext.getJobDetail().getKey().getName(),
                jobExecutionContext.getJobDetail().getKey().getGroup(),
                jobExecutionContext.getJobDetail().getJobClass().getName());
        // 记录执行的定时任务信息
        SysJobDetailLogEntity log = new SysJobDetailLogEntity();
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        // 获取任务类名、任务名称、任务组、任务参数数据、任务执行时间信息
        log.setJobName(jobDetail.getKey().getName());
        log.setJobGroup(jobDetail.getKey().getGroup());
        log.setClassName(jobDetail.getJobClass().getName());
        log.setStartTime(jobExecutionContext.getFireTime());
        log.setParams(JsonUtils.toJson(jobDetail.getJobDataMap()));
        sysJobDetailLogService.save(log);
        logTheadLocal.set(log);
    }

    /**
     * 由调度程序调用时JobDetail即将被执行(一个关联触发发生),但TriggerListener否决了它的执行。
     * @param jobExecutionContext 任务执行上下文
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {
        log.info("任务监听器触发： jobExecutionVetoed ----- 任务信息：【 任务名：[{}]、任务组：[{}]、任务类：[{}]】",
                jobExecutionContext.getJobDetail().getKey().getName(),
                jobExecutionContext.getJobDetail().getKey().getGroup(),
                jobExecutionContext.getJobDetail().getJobClass().getName());
        logTheadLocal.remove();
    }

    /**
     * 由调度程序调用JobDetail执行完毕后进行调用
     * @param jobExecutionContext 任务执行上下文
     * @param e 异常信息
     */
    @Override
    public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException e) {
        log.info("任务监听器触发： jobWasExecuted ----- 任务信息：【 任务名：[{}]、任务组：[{}]、任务类：[{}]】",
                jobExecutionContext.getJobDetail().getKey().getName(),
                jobExecutionContext.getJobDetail().getKey().getGroup(),
                jobExecutionContext.getJobDetail().getJobClass().getName());
        SysJobDetailLogEntity jobLog = logTheadLocal.get();
        if (jobLog == null) {
            return;
        }
        jobLog.setStatus(e == null);
        jobLog.setEndTime(new Date());
        jobLog.setTimes(jobExecutionContext.getJobRunTime());
        if (e != null) {
            jobLog.setException(e.getLocalizedMessage());
        }
        sysJobDetailLogService.updateById(jobLog);
        String[] strArray = jobLog.getJobName().split(":");
        if (strArray.length > 1) {
            String md5 = strArray[1];
            SysJobDetailEntity detail = sysJobDetailService.getByMd5(md5);
            if (detail != null) {
                detail.setNextExecutionTime(jobExecutionContext.getNextFireTime());
                detail.setLastExecutionStatus(jobLog.getStatus());
                sysJobDetailService.updateById(detail);
            }
        }
        logTheadLocal.remove();
    }
}
