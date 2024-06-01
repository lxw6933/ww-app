package com.ww.mall.seckill.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.excel.MallAbstractImportListener;
import com.ww.mall.seckill.entity.Demo;
import com.ww.mall.seckill.model.DemoModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

/**
 * @author ww
 * @create 2024-06-01 14:06
 * @description:
 */
@Slf4j
public class DemoImportListener extends MallAbstractImportListener<DemoModel> {

    private final MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);

    @Override
    protected void handleData(List<DemoModel> dataList) {
        List<Demo> demoList = BeanUtil.copyToList(dataList, Demo.class);
        mongoTemplate.insert(demoList, Demo.class);
    }

    @Override
    protected void handleErrorData(List<DemoModel> errorDataList) {
        System.out.println(errorDataList.size());
    }
}
