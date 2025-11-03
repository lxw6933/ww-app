package com.ww.app.disruptor;

import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用批量事件处理器
 * 
 * @author ww-framework
 */
@Slf4j
@Getter
public class TestBatchEventProcessor implements BatchEventProcessor<String> {

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger batchCount = new AtomicInteger(0);
    private final boolean throwException;

    public TestBatchEventProcessor() {
        this.throwException = false;
    }

    public TestBatchEventProcessor(boolean throwException) {
        this.throwException = throwException;
    }

    @Override
    public ProcessResult processBatch(EventBatch<String> batch) {
        try {
            int size = batch.getEvents().size();
            
            // 模拟异常
            if (throwException && batchCount.get() % 2 == 0) {
                throw new RuntimeException("测试异常：批量处理失败");
            }

            // 模拟批量处理耗时
            Thread.sleep(10);

            // 记录处理
            processedCount.addAndGet(size);
            batchCount.incrementAndGet();

            log.debug("批量处理: batchId={}, size={}, total={}", 
                    batch.getBatchId(), size, processedCount.get());

            return ProcessResult.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessResult.failure("批处理中断: " + e.getMessage());
        } catch (Exception e) {
            log.error("批量处理失败", e);
            return ProcessResult.failure("批处理失败: " + e.getMessage());
        }
    }
}
