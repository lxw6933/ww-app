package com.ww.framework.disruptor;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.EventProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DisruptorTemplate测试类
 *
 * @author ww-framework
 */
class DisruptorTemplateTest {

    private DisruptorTemplate<String> template;
    private AtomicInteger processedCount;
    private CountDownLatch latch;

    @BeforeEach
    void setUp() {
        processedCount = new AtomicInteger(0);
        latch = new CountDownLatch(10);

        // 创建测试处理器
        EventProcessor<String> processor = event -> {
            processedCount.incrementAndGet();
            latch.countDown();
            System.out.println("处理事件: " + event.getEventId() + ", 数据: " + event.getPayload());
            return ProcessResult.success("处理成功");
        };

        // 构建Template
        template = DisruptorTemplate.<String>builder()
                .ringBufferSize(1024)
                .consumerThreads(2)
                .batchSize(10)
                .waitStrategy("BLOCKING")
                .eventProcessor(processor)
                .build();

        // 启动
        template.start();
    }

    @AfterEach
    void tearDown() {
        if (template != null && template.isRunning()) {
            template.stop();
        }
    }

    @Test
    void testPublishSingleEvent() throws InterruptedException {
        // 发布单个事件
        Event<String> event = new Event<>("test", "Hello Disruptor");
        boolean success = template.publish(event);

        assertTrue(success, "事件发布应该成功");

        // 等待处理完成
        Thread.sleep(100);

        assertTrue(processedCount.get() > 0, "事件应该被处理");
    }

    @Test
    void testPublishMultipleEvents() throws InterruptedException {
        // 发布多个事件
        for (int i = 0; i < 10; i++) {
            Event<String> event = new Event<>("test", "Event-" + i);
            template.publish(event);
        }

        // 等待所有事件处理完成
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertTrue(completed, "所有事件应该在5秒内处理完成");
        assertEquals(10, processedCount.get(), "应该处理10个事件");
    }

    @Test
    void testPublishSimplified() throws InterruptedException {
        // 使用简化的发布方法
        boolean success = template.publish("test", "Simplified Event");

        assertTrue(success, "简化发布应该成功");

        Thread.sleep(100);

        assertTrue(processedCount.get() > 0, "事件应该被处理");
    }

    @Test
    void testAsyncPublish() throws InterruptedException {
        // 异步发布
        Event<String> event = new Event<>("test", "Async Event");
        template.publishAsync(event).thenAccept(result -> assertTrue(result, "异步发布应该成功"));

        Thread.sleep(200);

        assertTrue(processedCount.get() > 0, "异步事件应该被处理");
    }

    @Test
    void testTemplateStatus() {
        // 验证Template状态
        assertTrue(template.isRunning(), "Template应该处于运行状态");
        assertEquals(0, template.getPublishCount(), "初始发布计数应该为0");

        // 发布事件
        template.publish("test", "Status Test");

        assertEquals(1, template.getPublishCount(), "发布计数应该为1");
    }

    @Test
    void testQueueMetrics() {
        // 快速发布大量事件
        for (int i = 0; i < 100; i++) {
            template.publish("test", "Event-" + i);
        }

        // 检查队列指标
        long pending = template.getPendingCount();
        double utilization = template.getQueueUtilization();

        System.out.println("待处理事件数: " + pending);
        System.out.println("队列利用率: " + utilization + "%");

        assertTrue(pending >= 0, "待处理事件数应该大于等于0");
        assertTrue(utilization >= 0 && utilization <= 100, "队列利用率应该在0-100之间");
    }
}
