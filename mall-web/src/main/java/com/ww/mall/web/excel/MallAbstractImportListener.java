package com.ww.mall.web.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ww
 * @create 2024-03-12- 11:19
 * @description: excel导入抽象监听器
 */
@Slf4j
public abstract class MallAbstractImportListener<T> extends AnalysisEventListener<T> {

    // 是否异步处理导入数据
    protected boolean asyncStatus;

    // 最大一批数据量
    protected int MAX_COUNT = 3000;

    protected ExcelImportResultVO excelImportResultVO = new ExcelImportResultVO();

    protected final List<T> dataList = new ArrayList<>();
    protected final List<T> errorDataList = new ArrayList<>();

    protected ThreadPoolExecutor excelThreadPoolExecutor = null;

    protected final List<CompletableFuture<Void>> importTaskList = new ArrayList<>();

    public MallAbstractImportListener() {}

    public MallAbstractImportListener(ThreadPoolExecutor excelThreadPoolExecutor) {
        this.excelThreadPoolExecutor = excelThreadPoolExecutor;
        this.asyncStatus = true;
    }

    @Override
    public void invoke(T data, AnalysisContext analysisContext) {
        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        if (validData(data)) {
            dataList.add(data);
        } else {
            errorDataList.add(data);
        }
        if (dataList.size() > MAX_COUNT) {
            this.abstractHandleData();
        }
        if (errorDataList.size() > MAX_COUNT) {
            handleErrorData();
            errorDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        this.abstractHandleData();
        if (CollectionUtils.isNotEmpty(importTaskList)) {
            try {
                CompletableFuture.allOf(importTaskList.toArray(new CompletableFuture[]{}));
            } catch (Exception e) {
                throw new ApiException("导入异常");
            }
            importTaskList.clear();
        }
        log.info("数据处理完成");
    }

    private void abstractHandleData() {
        if (asyncStatus) {
            // 异步处理
            CompletableFuture<Void> task;
            if (excelThreadPoolExecutor == null) {
                task = CompletableFuture.runAsync(this::handleData).exceptionally((e) -> {
                    log.error("导入异常：{}", e.getMessage());
                    return null;
                });
            } else {
                task = CompletableFuture.runAsync(this::handleData, excelThreadPoolExecutor).exceptionally((e) -> {
                    log.error("导入异常：{}", e.getMessage());
                    return null;
                });
            }
            importTaskList.add(task);
        } else {
            handleData();
        }
        dataList.clear();
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
