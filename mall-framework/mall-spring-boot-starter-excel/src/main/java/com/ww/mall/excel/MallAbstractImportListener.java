package com.ww.mall.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.MallThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ww
 * @create 2024-03-12- 11:19
 * @description: excel导入抽象监听器
 */
@Slf4j
public abstract class MallAbstractImportListener<T> extends AnalysisEventListener<T> {

    private static final int MAX_COUNT = 10000;

    private static final ExecutorService excelThreadPoolExecutor = MallThreadUtil.initFixedThreadPoolExecutor("mall-excel", 20);

    private final ThreadLocal<ArrayList<T>> dataList = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<ArrayList<T>> errorDataList = ThreadLocal.withInitial(ArrayList::new);

    private final AtomicInteger count = new AtomicInteger(0);

    private final List<CompletableFuture<Void>> importTaskList = new ArrayList<>();

    private final AtomicInteger importErrorCount = new AtomicInteger(0);
    private final AtomicInteger importTotalCount = new AtomicInteger(0);

    public MallAbstractImportListener() {}

    @Override
    public void invoke(T data, AnalysisContext analysisContext) {
//        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        if (validData(data)) {
            dataList.get().add(data);
        } else {
            errorDataList.get().add(data);
        }
        if (dataList.get().size() >= MAX_COUNT) {
            this.asyncHandlerData();
        }
        if (errorDataList.get().size() >= MAX_COUNT) {
            @SuppressWarnings("unchecked")
            List<T> errorDataListTask = (ArrayList<T>) errorDataList.get().clone();
            errorDataList.remove();
            this.handleErrorData(errorDataListTask);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 处理剩余数据
        if (!dataList.get().isEmpty()) {
            this.asyncHandlerData();
        }
        // 处理剩余异常数据
        if (!errorDataList.get().isEmpty()) {
            @SuppressWarnings("unchecked")
            List<T> errorDataListTask = (ArrayList<T>) errorDataList.get().clone();
            errorDataList.remove();
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
        log.info("此次导入总数据量：[{}]异常数据量：[{}]", importTotalCount.get(), importErrorCount.get());
    }

    private void asyncHandlerData() {
        // 拷贝线程数据
        @SuppressWarnings("unchecked")
        List<T> dataListTask = (ArrayList<T>) dataList.get().clone();
        int size = dataListTask.size();
        // 清空线程数据
        dataList.remove();
        // 异步处理
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            log.info("第{}次插入{}条数据", count.incrementAndGet(), size);
            handleData(dataListTask);
        }, excelThreadPoolExecutor).exceptionally((e) -> {
            log.error("导入异常：{}", e.getMessage());
            handleErrorData(dataListTask);
            importErrorCount.addAndGet(size);
            return null;
        });
        importTaskList.add(task);
        importTotalCount.addAndGet(size);
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
