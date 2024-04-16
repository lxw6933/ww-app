package com.ww.mall.seckill.node;


import com.ww.mall.seckill.node.context.DemoContext;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("b")
public class BNode extends NodeComponent {
    @Override
    public void process() {
        // 获取传入参数
        Object requestData = this.getRequestData();
        System.out.println("参数：" + requestData);
        // 获取上下文对象
        DemoContext demo = this.getContextBean(DemoContext.class);
        System.out.println("上下文对象：" + demo);
        log.info("b");
    }
}
