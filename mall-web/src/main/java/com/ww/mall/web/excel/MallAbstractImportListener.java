package com.ww.mall.web.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-03-12- 11:19
 * @description: excel导入抽象监听器
 */
@Slf4j
public abstract class MallAbstractImportListener<T> extends AnalysisEventListener<T> {

    protected static final int MAX_COUNT = 10000;

    protected ExcelImportResultVO excelImportResultVO;

    protected final List<T> dataList = new ArrayList<>();
    protected final List<T> errorDataList = new ArrayList<>();

    @Override
    public void invoke(T data, AnalysisContext analysisContext) {
        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        if (validData(data)) {
            dataList.add(data);
        } else {
            errorDataList.add(data);
        }
        if (dataList.size() > MAX_COUNT) {
            handleData();
            dataList.clear();
        }
        if (errorDataList.size() > MAX_COUNT) {
            handleErrorData();
            errorDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        handleData();
        log.info("数据处理完成");
    }

    /**
     * 数据校验
     *
     * @return 正常：true
     */
    protected abstract boolean validData(T data);

    /**
     * 数据处理
     */
    protected abstract void handleData();

    /**
     * 异常数据处理
     */
    protected abstract void handleErrorData();

}
