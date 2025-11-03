package com.ww.app.disruptor;

import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.EventProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用事件处理器
 * 
 * @author ww-framework
 */
@Slf4j
@Getter
public class TestEventProcessor implements EventProcessor<String> {

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final List<String> processedEvents = new CopyOnWriteArrayList<>();
    private final boolean throwException;

    public TestEventProcessor() {
        this.throwException = false;
    }

    public TestEventProcessor(boolean throwException) {
        this.throwException = throwException;
    }

    @Override
    public ProcessResult process(Event<String> event) {
        try {
            // 模拟异常
            if (throwException && processedCount.get() % 3 == 0) {
                throw new RuntimeException("测试异常：模拟处理失败");
            }

            // 模拟处理耗时
            Thread.sleep(1);

            // 记录处理
            processedEvents.add(event.getPayload());
            processedCount.incrementAndGet();

            log.debug("处理事件: eventId={}, payload={}", event.getEventId(), event.getPayload());

            return ProcessResult.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessResult.failure("处理中断: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理事件失败", e);
            return ProcessResult.failure("处理失败: " + e.getMessage());
        }
    }
}
