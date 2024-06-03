package com.ww.mall.seckill.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.excel.MallAbstractImportListener;
import com.ww.mall.excel.vo.ExcelResultVO;
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

    private final ExcelResultVO result;

    public DemoImportListener(ExcelResultVO result) {
        this.result = result;
    }

    @Override
    protected void handleData(List<DemoModel> dataList) {
        List<Demo> demoList = BeanUtil.copyToList(dataList, Demo.class);
        mongoTemplate.insert(demoList, Demo.class);
        synchronized (result) {
            result.setSuccessNum(dataList.size() + result.getSuccessNum());
        }
    }

    @Override
    protected void handleErrorData(List<DemoModel> errorDataList) {
        System.out.println(errorDataList.size());
        synchronized (result) {
            result.setFailNum(errorDataList.size() + result.getFailNum());
        }
    }
}
