package com.ww.mall.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.MallThreadUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

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
public abstract class MallAbstractImportListener<T> extends AnalysisEventListener<T> {

    private static final ExecutorService excelThreadPoolExecutor = MallThreadUtil.initFixedThreadPoolExecutor("mall-excel", 20);

    private static final int MAX_COUNT = 1000;

    private final ArrayList<T> dataList = new ArrayList<>();
    private final ArrayList<T> errorDataList = new ArrayList<>();

    private final List<CompletableFuture<Void>> importTaskList = new ArrayList<>();

    @Getter
    protected int analysisTotalCount;

    @Getter
    protected int analysisErrorCount;

    public MallAbstractImportListener() {}

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
        if (dataList.size() >= MAX_COUNT) {
            this.asyncHandlerData();
        }
        if (errorDataList.size() >= MAX_COUNT) {
            @SuppressWarnings("unchecked")
            List<T> errorDataListTask = (ArrayList<T>) errorDataList.clone();
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
            @SuppressWarnings("unchecked")
            List<T> errorDataListTask = (ArrayList<T>) errorDataList.clone();
            errorDataList.clear();
            this.handleErrorData(errorDataListTask);
        }
        // 等待所有线程执行完成
        if (CollectionUtils.isNotEmpty(importTaskList)) {
            try {
                CompletableFuture.allOf(importTaskList.toArray(new CompletableFuture[]{})).get();
            } catch (Exception e) {
                throw new ApiException("导入异常");
            }
            importTaskList.clear();
        }
        log.info("此次解析总数据量：[{}] 解析异常数据量：[{}]", getAnalysisTotalCount(), getAnalysisErrorCount());
    }

    private void asyncHandlerData() {
        // 拷贝线程数据
        @SuppressWarnings("unchecked")
        List<T> dataListTask = (ArrayList<T>) dataList.clone();
        // 清空线程数据
        dataList.clear();
        // 异步处理
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> handleData(dataListTask), excelThreadPoolExecutor).exceptionally((e) -> {
            log.error("处理excel数据异常：{}", e.getMessage());
            handleErrorData(dataListTask);
            return null;
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
