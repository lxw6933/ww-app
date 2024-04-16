package com.ww.mall.seckill.node.context;

import com.yomahub.liteflow.context.ContextBean;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DemoContext {

    private String paramStr;

    private List<String> paramList;

    private Map<String, String> paramMap;
}
