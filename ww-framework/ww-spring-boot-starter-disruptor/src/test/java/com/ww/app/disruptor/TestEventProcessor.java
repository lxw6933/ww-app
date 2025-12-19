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

    private final boolean sleep;

    public TestEventProcessor() {
        this.throwException = false;
        this.sleep = false;
    }

    public TestEventProcessor(boolean throwException) {
        this.throwException = throwException;
        this.sleep = false;
    }

    public TestEventProcessor(boolean throwException, boolean sleep) {
        this.throwException = throwException;
        this.sleep = sleep;
    }

    @Override
    public ProcessResult process(Event<String> event) {
        try {
            // 先获取当前计数（在增加前）
            int currentCount = processedCount.getAndIncrement();
            
            // 模拟异常 - 使用获取到的计数判断
            if (throwException && currentCount % 3 == 0) {
                throw new RuntimeException("测试异常：模拟处理失败 (event #" + currentCount + ")");
            }

            if (sleep) {
                // 模拟业务处理耗时
                Thread.sleep(10);
            }

            // 记录处理（移除 Thread.sleep 以提高测试速度）
            processedEvents.add(event.getPayload());

            log.debug("处理事件: eventId={}, payload={}, count={}", event.getEventId(), event.getPayload(), currentCount);

            return ProcessResult.success();

        } catch (RuntimeException e) {
            // 业务异常，返回失败但不重置中断标志
            log.error("处理事件失败: {}", e.getMessage());
            return ProcessResult.failure("处理失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理事件异常", e);
            return ProcessResult.failure("处理异常: " + e.getMessage());
        }
    }
}
