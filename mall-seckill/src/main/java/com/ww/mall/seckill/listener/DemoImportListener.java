package com.ww.mall.seckill.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.excel.AbstractImportListener;
import com.ww.mall.excel.vo.ExcelResultVO;
import com.ww.mall.mongodb.handler.MongoBulkDataHandler;
import com.ww.mall.seckill.entity.Demo;
import com.ww.mall.seckill.model.DemoModel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author ww
 * @create 2024-06-01 14:06
 * @description:
 */
@Slf4j
public class DemoImportListener extends AbstractImportListener<DemoModel> {

    private final MongoBulkDataHandler<Demo> mongoBulkDataHandler = SpringUtil.getBean(new TypeReference<MongoBulkDataHandler<Demo>>() {});

    private final ExcelResultVO result;

    public DemoImportListener(ExcelResultVO result) {
        this.result = result;
    }

    @Override
    protected void handleData(List<DemoModel> dataList) {
        List<Demo> demoList = BeanUtil.copyToList(dataList, Demo.class);
        mongoBulkDataHandler.bulkSave(demoList);
    }

    @Override
    protected void handleErrorData(List<DemoModel> errorDataList) {
        System.out.println(errorDataList.size());
    }
}
