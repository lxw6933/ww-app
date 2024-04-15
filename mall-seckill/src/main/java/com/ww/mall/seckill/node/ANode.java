package com.ww.mall.seckill.node;


import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("a")
public class ANode extends NodeComponent {
    @Override
    public void process() {
        log.info("a");
    }
}
