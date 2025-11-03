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
            
            // 先增加批次计数
            int currentBatchCount = batchCount.getAndIncrement();
            
            // 模拟异常 - 根据批次大小判断（大于等于6个的批次失败）
            if (throwException && size >= 6) {
                throw new RuntimeException("测试异常：批量处理失败 (batch #" + currentBatchCount + ", size=" + size + ")");
            }

            // 记录处理（移除 Thread.sleep 以提高测试速度）
            processedCount.addAndGet(size);

            log.debug("批量处理: batchId={}, batchCount={}, size={}, total={}", 
                    batch.getBatchId(), currentBatchCount, size, processedCount.get());

            return ProcessResult.success();

        } catch (RuntimeException e) {
            // 业务异常，返回失败
            log.error("批量处理失败: {}", e.getMessage());
            return ProcessResult.failure("批处理失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("批量处理异常", e);
            return ProcessResult.failure("批处理异常: " + e.getMessage());
        }
    }
}
