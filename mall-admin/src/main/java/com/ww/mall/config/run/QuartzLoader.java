package com.ww.mall.config.run;

import com.ww.mall.mvc.entity.dto.SysJobDetailDTO;
import com.ww.mall.mvc.service.SysJobDetailService;
import com.ww.mall.quartz.QuartzManager;
import com.ww.mall.quartz.listener.GlobalJobListener;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.StringJoiner;

/**
 * @description: Spring启动后
 *  Quartz 初始化管理
 *  1.启动Quartz
 *  2.设置全局监听器
 *  3.加载数据库里面的定时任务
 * @author: ww
 * @create: 2021-05-13 19:47
 */
@Slf4j
@Component
public class QuartzLoader implements CommandLineRunner {

    @Resource
    private QuartzManager quartzManager;

    @Resource
    private GlobalJobListener globalJobListener;

    @Resource
    private SysJobDetailService sysJobDetailService;

    @Override
    public void run(String... args) throws Exception {
        // 设置任务执行全局监听器
        quartzManager.addGlobalListener(globalJobListener);
        // 获取所有可执行的任务
        List<SysJobDetailDTO> executionJobs = sysJobDetailService.listExecutionJob();
        // 获取所有停止的定时任务
        List<SysJobDetailDTO> stopJobs = sysJobDetailService.listStopJob();

        for (SysJobDetailDTO dto : executionJobs) {
            StringJoiner joiner = new StringJoiner(":")
                    .add(dto.getJobGroupName())
                    .add(dto.getMd5());
            String jobName = joiner.toString();
            try {
                // 将数据库中的可执行任务添加到quartz中
                quartzManager.addJob(jobName, dto.getJobGroupName(), jobName, dto.getJobGroupName(),
                        Class.forName(dto.getClassName()), dto.getCronExpression());
            } catch (ClassNotFoundException ex) {
                log.error("The {} class not fount", dto.getClassName(), ex);
                throw ex;
            }
        }

        for (SysJobDetailDTO dto : stopJobs) {
            StringJoiner joiner = new StringJoiner(":")
                    .add(dto.getJobGroupName())
                    .add(dto.getMd5());
            String jobName = joiner.toString();
            // 从quartz中移除暂停的任务
            quartzManager.removeJob(jobName, dto.getJobGroupName(), jobName, dto.getJobGroupName());
        }
        // 执行quartz中的所有任务
        quartzManager.startAllJob();
        log.info("启动任务调度器quartz完成");
    }

    /**
     * 系统关闭调用
     * @throws SchedulerException 调度器异常
     */
    @PreDestroy
    public void destroy() throws SchedulerException {
        quartzManager.stopAllJob();
        log.info("停止任务调度器quartz成功");
    }

}
