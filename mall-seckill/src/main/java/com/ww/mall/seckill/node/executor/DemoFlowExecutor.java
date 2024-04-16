package com.ww.mall.seckill.node.executor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.ww.mall.seckill.node.context.DemoContext;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;

@Slf4j
@Component
public class DemoFlowExecutor {

    @Resource
    private FlowExecutor flowExecutor;

    public void testConfig(){
        DemoContext context = new DemoContext();
        context.setParamStr("demoStr");
        context.setParamList(Collections.singletonList("paramList1"));
        HashMap<String, String> map = Maps.newHashMap();
        map.put("mapKey", "mapValue");
        context.setParamMap(map);
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", context);
        // 判断流程是否执行成功
        boolean isSuccess = response.isSuccess();
        if (!isSuccess) {
            // 获取流程执行异常信息
            Exception e = response.getCause();
            log.error("流程执行异常", e);
        }
        DemoContext responseContext = response.getContextBean(DemoContext.class);
        // 对于只有一个上下文的response来说，用下面这句也是等价的
        //CustomContext context = response.getFirstContextBean();
    }

}
