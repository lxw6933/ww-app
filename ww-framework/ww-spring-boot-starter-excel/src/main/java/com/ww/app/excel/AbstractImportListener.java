package com.ww.app.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ww.app.common.utils.ThreadUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * @author ww
 * @create 2024-03-12- 11:19
 * @description: excel导入抽象监听器
 */
@Slf4j
public abstract class AbstractImportListener<T> extends AnalysisEventListener<T> {

    private final int BATCH_SIZE;
    private final ExecutorService excelThreadPoolExecutor;

    private final ArrayList<T> dataList = new ArrayList<>();
    private final ArrayList<T> errorDataList = new ArrayList<>();
    private final List<CompletableFuture<Void>> importTaskList = new ArrayList<>();

    @Getter
    protected int analysisTotalCount;

    @Getter
    protected int analysisErrorCount;

    public AbstractImportListener() {
        this.BATCH_SIZE = 1000;
        this.excelThreadPoolExecutor = ThreadUtil.initFixedThreadPoolExecutor("ww-excel", 20);
    }

    public AbstractImportListener(int batchSize, ExecutorService executorService) {
        this.BATCH_SIZE = batchSize;
        this.excelThreadPoolExecutor = executorService;
    }

    @Override
    public void invoke(T data, AnalysisContext analysisContext) {
        analysisTotalCount++;
//        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        if (validData(data)) {
            dataList.add(data);
        } else {
            analysisErrorCount++;
            errorDataList.add(data);
        }
        if (dataList.size() >= BATCH_SIZE) {
            this.asyncHandlerData();
        }
        if (errorDataList.size() >= BATCH_SIZE) {
            List<T> errorDataListTask = new ArrayList<>(errorDataList);
            errorDataList.clear();
            this.handleErrorData(errorDataListTask);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 处理剩余数据
        if (!dataList.isEmpty()) {
            this.asyncHandlerData();
        }
        // 处理剩余异常数据
        if (!errorDataList.isEmpty()) {
            List<T> errorDataListTask = new ArrayList<>(errorDataList);;
            errorDataList.clear();
            this.handleErrorData(errorDataListTask);
        }
        // 等待所有线程执行完成
//        CompletableFuture.allOf(importTaskList.toArray(new CompletableFuture[0])).get();
        CompletableFuture.allOf(importTaskList.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
            importTaskList.clear();
            log.info("此次解析总数据量：[{}] 解析异常数据量：[{}]", getAnalysisTotalCount(), getAnalysisErrorCount());
        }, excelThreadPoolExecutor);
    }

    private void asyncHandlerData() {
        // 拷贝线程数据
        List<T> dataListTask = new ArrayList<>(dataList);
        // 清空线程数据
        dataList.clear();
        // 异步处理
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> handleData(dataListTask), excelThreadPoolExecutor).handle((res, ex) -> {
            if (ex != null) {
                log.error("处理excel数据异常", ex);
                handleErrorData(dataListTask);
            }
            return res;
        });
        importTaskList.add(task);
    }

    /**
     * 数据校验
     *
     * @return 正常：true
     */
    protected boolean validData(T data) {
        return true;
    }

    /**
     * 数据处理
     */
    protected abstract void handleData(List<T> dataList);

    /**
     * 异常数据处理
     */
    protected abstract void handleErrorData(List<T> errorDataList);

}
