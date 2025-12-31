package com.ww.app.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ww.app.common.utils.ThreadUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2024-03-12- 11:19
 * @description: excel导入抽象监听器
 */
@Slf4j
public abstract class AbstractImportListener<T> extends AnalysisEventListener<T> {

    private final int BATCH_SIZE;
    private final ExecutorService excelThreadPoolExecutor;

    private final List<T> dataList = new CopyOnWriteArrayList<>();
    private final List<T> errorDataList = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<Void>> importTaskList = new CopyOnWriteArrayList<>();

    @Getter
    protected final AtomicLong analysisTotalCount = new AtomicLong(0);

    @Getter
    protected final AtomicLong analysisErrorCount = new AtomicLong(0);
    
    private final boolean shouldShutdownExecutor;

    public AbstractImportListener() {
        this.BATCH_SIZE = 1000;
        this.excelThreadPoolExecutor = ThreadUtil.initFixedThreadPoolExecutor("ww-excel", 20);
        this.shouldShutdownExecutor = true;
    }

    public AbstractImportListener(int batchSize, ExecutorService executorService) {
        this.BATCH_SIZE = batchSize;
        this.excelThreadPoolExecutor = executorService;
        this.shouldShutdownExecutor = false;
    }

    @Override
    public void invoke(T data, AnalysisContext analysisContext) {
        analysisTotalCount.incrementAndGet();
//        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        if (validData(data)) {
            dataList.add(data);
            if (dataList.size() >= BATCH_SIZE) {
                this.asyncHandlerData();
            }
        } else {
            analysisErrorCount.incrementAndGet();
            errorDataList.add(data);
            if (errorDataList.size() >= BATCH_SIZE) {
                this.asyncHandlerErrorData();
            }
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
            this.asyncHandlerErrorData();
        }
        // 等待所有线程执行完成
        try {
            if (!importTaskList.isEmpty()) {
                CompletableFuture.allOf(importTaskList.toArray(new CompletableFuture[0])).get();
            }
            log.info("此次解析总数据量：[{}] 解析异常数据量：[{}]", 
                    analysisTotalCount.get(), analysisErrorCount.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待任务完成时被中断", e);
        } catch (ExecutionException e) {
            log.error("等待任务完成时发生异常", e);
        } finally {
            importTaskList.clear();
            // 如果线程池是当前类创建的，需要关闭
            if (shouldShutdownExecutor && excelThreadPoolExecutor != null) {
                shutdownExecutor();
            }
            // 调用完成回调
            onImportCompleted();
        }
    }

    private void asyncHandlerData() {
        // 拷贝线程数据
        List<T> dataListTask = new ArrayList<>(dataList);
        // 清空线程数据
        dataList.clear();
        // 异步处理
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            try {
                handleData(dataListTask);
                onBatchDataHandled(dataListTask.size(), true);
            } catch (Exception e) {
                log.error("处理excel数据异常", e);
                onBatchDataHandled(dataListTask.size(), false);
                asyncHandlerErrorData(dataListTask);
            }
        }, excelThreadPoolExecutor);
        importTaskList.add(task);
    }
    
    private void asyncHandlerErrorData() {
        // 拷贝错误数据
        List<T> errorDataListTask = new ArrayList<>(errorDataList);
        // 清空错误数据
        errorDataList.clear();
        // 异步处理错误数据
        asyncHandlerErrorData(errorDataListTask);
    }
    
    private void asyncHandlerErrorData(List<T> errorDataListTask) {
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            try {
                handleErrorData(errorDataListTask);
                onBatchErrorDataHandled(errorDataListTask.size());
            } catch (Exception e) {
                log.error("处理excel错误数据异常", e);
            }
        }, excelThreadPoolExecutor);
        importTaskList.add(task);
    }
    
    private void shutdownExecutor() {
        try {
            excelThreadPoolExecutor.shutdown();
            if (!excelThreadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Excel线程池未在60秒内正常关闭，强制关闭");
                excelThreadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("关闭Excel线程池时被中断", e);
            excelThreadPoolExecutor.shutdownNow();
        }
    }

    /**
     * 数据校验
     *
     * @param data 待校验的数据
     * @return 正常：true，异常：false
     */
    protected boolean validData(T data) {
        return true;
    }

    /**
     * 数据处理
     *
     * @param dataList 待处理的数据列表
     */
    protected abstract void handleData(List<T> dataList);

    /**
     * 异常数据处理
     *
     * @param errorDataList 异常数据列表
     */
    protected abstract void handleErrorData(List<T> errorDataList);

    /**
     * 批量数据处理完成回调
     *
     * @param batchSize 批次大小
     * @param success 是否成功
     */
    protected void onBatchDataHandled(int batchSize, boolean success) {
        // 子类可重写此方法实现自定义逻辑
    }

    /**
     * 批量错误数据处理完成回调
     *
     * @param batchSize 批次大小
     */
    protected void onBatchErrorDataHandled(int batchSize) {
        // 子类可重写此方法实现自定义逻辑
    }

    /**
     * 导入完成回调
     */
    protected void onImportCompleted() {
        // 子类可重写此方法实现自定义逻辑
    }

    /**
     * 获取处理进度（已处理数量 / 总数量）
     *
     * @return 进度百分比，0-100
     */
    public double getProgress() {
        long total = analysisTotalCount.get();
        if (total == 0) {
            return 0.0;
        }
        // 估算：总数量 - 待处理数量（dataList + errorDataList + 正在处理的任务）
        long processed = total - dataList.size() - errorDataList.size();
        return Math.min(100.0, (processed * 100.0) / total);
    }

}
