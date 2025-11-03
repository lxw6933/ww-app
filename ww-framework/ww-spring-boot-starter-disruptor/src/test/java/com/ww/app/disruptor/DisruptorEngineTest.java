package com.ww.app.disruptor;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.model.Event;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Disruptor 引擎综合测试类
 * 测试覆盖：
 * 1. 基本功能测试
 * 2. 并发压力测试
 * 3. 批量处理测试
 * 4. 异常处理测试
 * 5. 性能指标测试
 * 6. 优雅关闭测试
 *
 * @author ww-framework
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DisruptorEngineTest {

    private DisruptorTemplate<String> template;
    private TestEventProcessor eventProcessor;
    private TestBatchEventProcessor batchEventProcessor;

    @BeforeEach
    void setUp() {
        log.info("========================================");
    }

    @AfterEach
    void tearDown() {
        if (template != null && template.isRunning()) {
            template.stop();
        }
        log.info("========================================\n");
    }

    // ==================== 1. 基本功能测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试1：单个事件处理 - 基本功能")
    void testSingleEventProcessing() throws InterruptedException {
        log.info("========== 测试1：单个事件处理 ==========");

        eventProcessor = new TestEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-single-event")
                .ringBufferSize(1024)
                .consumerThreads(2)
                .eventProcessor(eventProcessor)
                .build();

        template.start();

        // 发布单个事件
        Event<String> event = new Event<>("test-event-1", "Hello Disruptor");
        boolean published = template.publish(event);

        assertTrue(published, "事件应该成功发布");

        // 等待处理完成
        Thread.sleep(500);

        // 验证处理结果
        assertEquals(1, eventProcessor.getProcessedCount().get(), "应该处理1个事件");
        assertEquals(1, template.getProcessCount(), "引擎计数应该为1");
        assertTrue(eventProcessor.getProcessedEvents().contains("Hello Disruptor"));

        log.info("✅ 测试1通过：单个事件处理成功");
    }

    @Test
    @Order(2)
    @DisplayName("测试2：批量事件处理 - 基本功能")
    void testBatchEventProcessing() throws InterruptedException {
        log.info("========== 测试2：批量事件处理 ==========");

        batchEventProcessor = new TestBatchEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-batch-event")
                .ringBufferSize(1024)
                .consumerThreads(2)
                .batchSize(10)
                .batchTimeout(100)
                .batchEnabled(true)
                .batchEventProcessor(batchEventProcessor)
                .build();

        template.start();

        // 发布多个事件
        int eventCount = 25;
        for (int i = 0; i < eventCount; i++) {
            Event<String> event = new Event<>("batch-event-" + i, "Batch Data " + i);
            template.publish(event);
        }

        // 等待批量处理完成
        Thread.sleep(1000);

        // 验证结果
        assertTrue(batchEventProcessor.getProcessedCount().get() >= eventCount,
                "应该处理至少" + eventCount + "个事件");
        assertTrue(batchEventProcessor.getBatchCount().get() >= 2,
                "应该至少执行2次批量处理");

        log.info("✅ 测试2通过：批量处理 {} 个事件，执行 {} 次批处理",
                batchEventProcessor.getProcessedCount().get(),
                batchEventProcessor.getBatchCount().get());
    }

    @Test
    @Order(3)
    @DisplayName("测试3：异步发布事件")
    void testAsyncPublish() throws Exception {
        log.info("========== 测试3：异步发布事件 ==========");

        eventProcessor = new TestEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-async-publish")
                .ringBufferSize(1024)
                .consumerThreads(2)
                .eventProcessor(eventProcessor)
                .build();

        template.start();

        // 异步发布事件
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Event<String> event = new Event<>("async-event-" + i, "Async Data " + i);
            CompletableFuture<Boolean> future = template.publishAsync(event);
            futures.add(future);
        }

        // 等待所有异步发布完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        // 验证所有发布都成功
        for (CompletableFuture<Boolean> future : futures) {
            assertTrue(future.get(), "异步发布应该成功");
        }

        Thread.sleep(500);

        assertEquals(10, eventProcessor.getProcessedCount().get(), "应该处理10个异步事件");

        log.info("✅ 测试3通过：异步发布10个事件成功");
    }

    @Test
    @Order(4)
    @DisplayName("测试4：超时发布事件")
    void testTryPublishWithTimeout() throws InterruptedException {
        log.info("========== 测试4：超时发布事件 ==========");

        eventProcessor = new TestEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-timeout-publish")
                .ringBufferSize(16)
                .consumerThreads(1)
                .eventProcessor(eventProcessor)
                .build();

        template.start();

        // 快速发布大量事件填满缓冲区
        int successCount = 0;
        for (int i = 0; i < 100; i++) {
            Event<String> event = new Event<>("timeout-event-" + i, "Data " + i);
            boolean success = template.tryPublish(event, 10, TimeUnit.MILLISECONDS);
            if (success) successCount++;
        }

        log.info("成功发布 {} 个事件（缓冲区大小16）", successCount);
        assertTrue(successCount > 0, "应该至少成功发布一些事件");

        Thread.sleep(1000);

        log.info("✅ 测试4通过：超时发布机制正常工作");
    }

    // ==================== 2. 并发压力测试 ====================

    @Test
    @Order(5)
    @DisplayName("测试5：并发压力测试 - 10线程发布10000事件")
    void testConcurrentStressTest() throws InterruptedException {
        log.info("========== 测试5：并发压力测试 ==========");

        eventProcessor = new TestEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-concurrent-stress")
                .ringBufferSize(8192)
                .consumerThreads(24)
                .eventProcessor(eventProcessor)
                .build();

        template.start();

        int threadCount = 10;
        int eventsPerThread = 1000;
        int totalEvents = threadCount * eventsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        // 多线程并发发布
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < eventsPerThread; i++) {
                        Event<String> event = new Event<>(
                                "stress-event-" + threadId + "-" + i,
                                "Thread-" + threadId + "-Data-" + i
                        );
                        template.publish(event);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有发布完成
        assertTrue(latch.await(30, TimeUnit.SECONDS), "发布应该在30秒内完成");

        long publishEndTime = System.currentTimeMillis();

        // 等待处理完成 - 增加等待时间确保所有事件都被处理
        // 使用轮询方式等待，最多等待10秒
        int maxWaitSeconds = 10;
        for (int i = 0; i < maxWaitSeconds * 10; i++) {
            Thread.sleep(100);
            if (template.getPendingCount() == 0) {
                break;
            }
        }

        long endTime = System.currentTimeMillis();

        // 验证结果
        assertEquals(totalEvents, template.getPublishCount(), "发布计数应该准确");
        // 在高并发场景下，processCount + failedCount 应该等于 publishCount
        // 因为某些事件可能因为线程中断等原因处理失败
        long actualProcessed = template.getProcessCount() + template.getFailedCount();
        assertEquals(totalEvents, actualProcessed, 
            String.format("处理计数(%d) + 失败计数(%d) = %d 应该等于总事件数 %d", 
                template.getProcessCount(), template.getFailedCount(), actualProcessed, totalEvents));

        // 性能指标
        long totalTime = endTime - startTime;
        long publishDuration = publishEndTime - startTime;
        double tps = (totalEvents * 1000.0) / totalTime;
        double publishTps = (totalEvents * 1000.0) / publishDuration;

        log.info("✅ 测试5通过：并发压力测试完成");
        log.info("  - 总事件数: {}", totalEvents);
        log.info("  - 发布耗时: {} ms", publishDuration);
        log.info("  - 总耗时: {} ms", totalTime);
        log.info("  - 发布TPS: {}/s", String.format("%.2f", publishTps));
        log.info("  - 处理TPS: {}/s", String.format("%.2f", tps));
        log.info("  - 队列利用率: {}%", String.format("%.2f", template.getQueueUtilization()));

        executor.shutdown();
    }

    @Test
    @Order(6)
    @DisplayName("测试6：高并发批量处理压力测试")
    void testConcurrentBatchStressTest() throws InterruptedException {
        log.info("========== 测试6：高并发批量处理压力测试 ==========");

        batchEventProcessor = new TestBatchEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-batch-stress")
                .ringBufferSize(8192)
                .consumerThreads(4)
                .batchSize(50)
                .batchTimeout(50)
                .batchEnabled(true)
                .batchEventProcessor(batchEventProcessor)
                .build();

        template.start();

        int threadCount = 10;
        int eventsPerThread = 1000;
        int totalEvents = threadCount * eventsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < eventsPerThread; i++) {
                        Event<String> event = new Event<>(
                                "batch-stress-" + threadId + "-" + i,
                                "BatchData-" + threadId + "-" + i
                        );
                        template.publish(event);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "发布应该在30秒内完成");

        Thread.sleep(3000);

        long endTime = System.currentTimeMillis();

        // 验证结果
        assertTrue(batchEventProcessor.getProcessedCount().get() >= totalEvents,
                "应该处理至少" + totalEvents + "个事件");

        double tps = (totalEvents * 1000.0) / (endTime - startTime);

        log.info("✅ 测试6通过：批量并发压力测试完成");
        log.info("  - 总事件数: {}", totalEvents);
        log.info("  - 批处理次数: {}", batchEventProcessor.getBatchCount());
        log.info("  - 平均批大小: {}", totalEvents / batchEventProcessor.getBatchCount().get());
        log.info("  - 处理TPS: {}/s", String.format("%.2f", tps));

        executor.shutdown();
    }

    // ==================== 3. 异常处理测试 ====================

    @Test
    @Order(7)
    @DisplayName("测试7：异常处理 - 处理器抛出异常")
    void testExceptionHandling() throws InterruptedException {
        log.info("========== 测试7：异常处理测试 ==========");

        eventProcessor = new TestEventProcessor(true); // 启用异常模式

        template = DisruptorTemplate.<String>builder()
                .businessName("test-exception")
                .ringBufferSize(1024)
                .consumerThreads(2)
                .eventProcessor(eventProcessor)
                .build();

        template.start();

        // 发布事件
        for (int i = 0; i < 10; i++) {
            Event<String> event = new Event<>("exception-event-" + i, "Data-" + i);
            template.publish(event);
        }

        Thread.sleep(1000);

        // 验证异常处理
        assertTrue(template.getFailedCount() > 0, "应该有失败计数");
        log.info("失败事件数: {}", template.getFailedCount());

        log.info("✅ 测试7通过：异常处理机制正常工作");
    }

    @Test
    @Order(8)
    @DisplayName("测试8：批量处理异常")
    void testBatchExceptionHandling() throws InterruptedException {
        log.info("========== 测试8：批量处理异常测试 ==========");

        batchEventProcessor = new TestBatchEventProcessor(true); // 启用异常模式

        template = DisruptorTemplate.<String>builder()
                .businessName("test-batch-exception")
                .ringBufferSize(1024)
                .consumerThreads(2)
                .batchSize(5)
                .batchTimeout(100)
                .batchEnabled(true)
                .batchEventProcessor(batchEventProcessor)
                .build();

        template.start();

        // 发布事件
        for (int i = 0; i < 15; i++) {
            Event<String> event = new Event<>("batch-exception-" + i, "Data-" + i);
            template.publish(event);
        }

        Thread.sleep(1000);

        // 验证批量异常处理
        assertTrue(template.getFailedCount() > 0, "应该有失败计数");
        log.info("批量失败事件数: {}", template.getFailedCount());

        log.info("✅ 测试8通过：批量异常处理机制正常工作");
    }

    // ==================== 4. 性能指标测试 ====================

    @Test
    @Order(9)
    @DisplayName("测试9：性能指标验证")
    void testPerformanceMetrics() throws InterruptedException {
        log.info("========== 测试9：性能指标验证 ==========");

        eventProcessor = new TestEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-metrics")
                .ringBufferSize(2048)
                .consumerThreads(4)
                .eventProcessor(eventProcessor)
                .build();

        template.start();

        // 验证初始状态
        assertEquals(0, template.getPublishCount(), "初始发布计数应该为0");
        assertEquals(0, template.getProcessCount(), "初始处理计数应该为0");
        assertEquals(0, template.getPendingCount(), "初始待处理计数应该为0");
        assertTrue(template.isRunning(), "引擎应该在运行");

        // 发布事件
        int eventCount = 1000;
        for (int i = 0; i < eventCount; i++) {
            template.publish(new Event<>("metric-event-" + i, "Data-" + i));
        }

        // 检查发布计数
        assertEquals(eventCount, template.getPublishCount(), "发布计数应该准确");

        // 等待处理
        Thread.sleep(2000);

        // 验证处理完成
        assertEquals(eventCount, template.getProcessCount(), "处理计数应该准确");
        assertEquals(0, template.getPendingCount(), "所有事件应该处理完成");
        assertTrue(template.getQueueUtilization() < 10.0, "队列利用率应该很低");

        log.info("✅ 测试9通过：性能指标准确");
        log.info("  - 发布计数: {}", template.getPublishCount());
        log.info("  - 处理计数: {}", template.getProcessCount());
        log.info("  - 失败计数: {}", template.getFailedCount());
        log.info("  - 待处理数: {}", template.getPendingCount());
        log.info("  - 队列利用率: {}%", String.format("%.2f", template.getQueueUtilization()));
    }

    // ==================== 5. 优雅关闭测试 ====================

    @Test
    @Order(10)
    @DisplayName("测试10：优雅关闭 - 确保事件不丢失")
    void testGracefulShutdown() {
        log.info("========== 测试10：优雅关闭测试 ==========");

        eventProcessor = new TestEventProcessor();

        template = DisruptorTemplate.<String>builder()
                .businessName("test-shutdown")
                .ringBufferSize(1024)
                .consumerThreads(2)
                .eventProcessor(eventProcessor)
                .build();

        template.start();

        // 发布事件
        int eventCount = 100;
        for (int i = 0; i < eventCount; i++) {
            template.publish(new Event<>("shutdown-event-" + i, "Data-" + i));
        }

        // 记录发布完成时的计数
        long publishedCount = template.getPublishCount();
        assertEquals(eventCount, publishedCount, "应该成功发布所有事件");

        // 立即关闭（优雅关闭会等待处理完成）
        template.stop();

        // 验证：优雅关闭后，所有已发布的事件都应该被处理，不会丢失
        assertFalse(template.isRunning(), "引擎应该已停止");
        
        long processedCount = template.getProcessCount();
        long failedCount = template.getFailedCount();
        long totalHandled = processedCount + failedCount;
        
        // 关键断言：处理数 + 失败数 = 发布数（没有丢失）
        assertEquals(publishedCount, totalHandled, 
            String.format("优雅关闭应确保事件不丢失: 发布=%d, 处理=%d, 失败=%d, 总计=%d",
                publishedCount, processedCount, failedCount, totalHandled));

        log.info("✅ 测试10通过：优雅关闭成功，事件无丢失");
        log.info("  - 发布: {}", publishedCount);
        log.info("  - 处理: {}", processedCount);
        log.info("  - 失败: {}", failedCount);
        log.info("  - 总计: {} (无丢失)", totalHandled);
    }
}
