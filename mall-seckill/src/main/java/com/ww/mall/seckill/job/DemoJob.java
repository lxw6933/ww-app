
package com.ww.mall.seckill.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoJob {

    @XxlJob("demoJobHandler")
    public void demoJobHandler() {
        log.info("demo job log");
        XxlJobHelper.log("mall xxl-job demo");
    }

}
