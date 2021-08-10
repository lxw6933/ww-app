package com.ww.mall.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.NameMatcher;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

/**
 * @description: 任务调度管理类
 * @author: ww
 * @create: 2021-05-13 16:33
 */
@Slf4j
@Component
public class QuartzManager {

    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;

    /**
     * 动态添加定时任务
     *
     * @param jobName          任务名
     * @param jobGroupName     任务组
     * @param triggerName      触发器
     * @param triggerGroupName 触发器组
     * @param jobClass         任务类
     * @param cronExpression   cron表达式异常
     * @return Date 执行日期
     */
    public Date addJob(String jobName,
                       String jobGroupName,
                       String triggerName,
                       String triggerGroupName,
                       Class jobClass,
                       String cronExpression,
                       Map<String, Object> params) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
        try {
            if (scheduler.checkExists(triggerKey)) {
                /* 停止触发器 */
                scheduler.pauseTrigger(triggerKey);
                /* 移除触发器 */
                scheduler.unscheduleJob(triggerKey);
                JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
                if (scheduler.checkExists(jobKey)) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (SchedulerException e) {
            log.warn("动态添加定时任务清除已有任务发生错误：" + e.getMessage());
            throw e;
        }
        /* job定义： 任务名，任务组，任务执行类 */
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
        /* 触发器 */
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, triggerGroupName)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
        if (params != null) {
            jobDetail.getJobDataMap().putAll(params);
        }
        try {
            return scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 动态添加定时任务
     *
     * @param jobName          任务名
     * @param jobGroupName     任务组
     * @param triggerName      触发器
     * @param triggerGroupName 触发器组
     * @param jobClass         任务类
     * @param cronExpression   corn表达式异常
     * @return 执行日期
     */
    public Date addJob(String jobName,
                       String jobGroupName,
                       String triggerName,
                       String triggerGroupName,
                       Class jobClass,
                       String cronExpression) throws SchedulerException {
        return addJob(jobName, jobGroupName, triggerName, triggerGroupName, jobClass, cronExpression, null);
    }

    /**
     * 启动所有定时任务
     */
    public void startAllJob() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            if (!scheduler.isStarted()) {
                scheduler.start();
            }
        } catch (SchedulerException e) {
            log.warn("start job failed");
            throw e;
        }
    }

    /**
     * 停止所有定时任务
     */
    public void stopAllJob() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            log.info("stop job failed");
            throw e;
        }
    }

    /**
     * 修改任务时间
     *
     * @param triggerName      触发器名字
     * @param triggerGroupName 触发器组名
     * @param corn             corn表达式
     * @throws SchedulerException 调度器异常
     */
    public void modifyJobTime(String triggerName, String triggerGroupName, String corn) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
        CronTrigger trigger;
        try {
            trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        } catch (SchedulerException e) {
            log.warn("触发器不存在");
            throw e;
        }
        String oldTime = trigger.getCronExpression();
        if (!oldTime.equalsIgnoreCase(corn)) {
            // 触发器
            TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
            // 触发器名,触发器组
            triggerBuilder.withIdentity(triggerName, triggerGroupName);
            triggerBuilder.startNow();
            // 触发器时间设定
            triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(corn));
            // 创建Trigger对象
            trigger = (CronTrigger) triggerBuilder.build();
            // 方式一 ：修改一个任务的触发时间
            scheduler.rescheduleJob(triggerKey, trigger);
        }
    }

    /**
     * 添加任务监听器（全局）
     *
     * @param jobListener jobListener
     * @throws SchedulerException 调度器异常
     */
    public void addGlobalListener(JobListener jobListener) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        ListenerManager listenerManager = scheduler.getListenerManager();
        listenerManager.addJobListener(jobListener);
    }

    /**
     * 添加具体任务监听器
     *
     * @param jobListener jobListener
     * @param jobName     任务名称
     * @throws SchedulerException 调度器异常
     */
    public void addJobListener(JobListener jobListener, String jobName) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        ListenerManager listenerManager = scheduler.getListenerManager();
        listenerManager.addJobListener(jobListener, NameMatcher.jobNameEquals(jobName));
    }

    /**
     * 添加具体任务组监听器
     *
     * @param jobListener  jobListener
     * @param jobGroupName 任务组名字
     * @throws SchedulerException 调度器异常
     */
    public void addJobGroupListener(JobListener jobListener, String jobGroupName) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        ListenerManager listenerManager = scheduler.getListenerManager();
        listenerManager.addJobListener(jobListener, GroupMatcher.groupEquals(jobGroupName));
    }

    /**
     * 清除所有任务与触发器
     */
    public void removeAllJob() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            log.warn("clear all Job failed");
            throw e;
        }
    }

    /**
     * 移除某个任务
     *
     * @param jobName          任务名称
     * @param jobGroupName     任务组名称
     * @param triggerName      触发器名称
     * @param triggerGroupName 触发器组
     */
    public void removeJob(String jobName, String jobGroupName, String triggerName, String triggerGroupName) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, triggerGroupName);
        JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            if (scheduler.checkExists(triggerKey)) {
                scheduler.pauseTrigger(triggerKey);
                if (scheduler.checkExists(jobKey)) {
                    scheduler.unscheduleJob(triggerKey);
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (SchedulerException e) {
            log.warn("remove Job jobName: {} jobGroupName: {} triggerName: {} triggerGroupName:{} failed", jobName, jobGroupName, triggerName, triggerGroupName);
            throw e;
        }

    }

    /**
     * 暂停某个任务
     *
     * @param jobName      任务名称
     * @param jobGroupName 任务组名称
     */
    public void pauseJob(String jobName, String jobGroupName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.pauseJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.warn("pause Job jobName: {} jobGroupName: {} failed", jobName, jobGroupName);
            throw e;
        }
    }

    /**
     * 恢复某个任务
     *
     * @param jobName      任务名称
     * @param jobGroupName 任务组名称
     */
    public void resumeJob(String jobName, String jobGroupName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.resumeJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.warn("resume Job jobName: {} jobGroupName: {} failed", jobName, jobGroupName);
            throw e;
        }
    }


}
