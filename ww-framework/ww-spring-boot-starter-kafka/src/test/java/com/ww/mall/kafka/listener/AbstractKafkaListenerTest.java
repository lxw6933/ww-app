package com.ww.mall.kafka.listener;

import com.ww.mall.kafka.exception.KafkaException;
import com.ww.mall.kafka.template.KafkaOperations;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AbstractKafkaListener单元测试
 */
@ExtendWith(MockitoExtension.class)
public class AbstractKafkaListenerTest {

    @Mock
    private ConsumerRecord<String, String> mockRecord;
    
    @Mock
    private Acknowledgment mockAck;
    
    @Mock
    private KafkaOperations mockKafkaOperations;
    
    private TestKafkaListener listener;
    
    private Headers testHeaders;
    
    @BeforeEach
    public void setup() {
        listener = new TestKafkaListener();
        listener.setKafkaOperations(mockKafkaOperations);
        
        // 设置模拟头信息
        testHeaders = new RecordHeaders();
        testHeaders.add(new RecordHeader("X-Trace-Id", "test-trace-id".getBytes(StandardCharsets.UTF_8)));
        testHeaders.add(new RecordHeader("X-Test-Header", "test-value".getBytes(StandardCharsets.UTF_8)));
        
        // 设置模拟消费记录
        when(mockRecord.topic()).thenReturn("test-topic");
        when(mockRecord.partition()).thenReturn(0);
        when(mockRecord.offset()).thenReturn(100L);
        when(mockRecord.key()).thenReturn("test-key");
        when(mockRecord.value()).thenReturn("test-value");
        when(mockRecord.headers()).thenReturn(testHeaders);
    }
    
    @Test
    public void testProcessRecord() {
        // 执行测试
        listener.processRecord(mockRecord, mockAck);
        
        // 验证结果
        assertTrue(listener.isBeforeProcessCalled());
        assertTrue(listener.isProcessMessageCalled());
        assertTrue(listener.isAfterProcessCalled());
        assertEquals("test-key", listener.getProcessedKey());
        assertEquals("test-value", listener.getProcessedValue());
        
        // 验证确认
        verify(mockAck, times(1)).acknowledge();
    }
    
    @Test
    public void testProcessRecordWithError() {
        // 设置测试监听器抛出异常
        listener.setShouldThrowException(true);
        listener.setRetryCount(2); // 设置当前重试次数为2，已达到最大重试次数
        
        // 执行测试
        listener.processRecord(mockRecord, mockAck);
        
        // 验证结果
        assertTrue(listener.isBeforeProcessCalled());
        assertTrue(listener.isProcessMessageCalled());
        assertFalse(listener.isAfterProcessCalled()); // 异常后不应调用afterProcess
        assertEquals("test-key", listener.getProcessedKey());
        assertEquals("test-value", listener.getProcessedValue());
        assertTrue(listener.isErrorHandled());
        
        // 验证确认 - 超过重试次数，应确认消息
        verify(mockAck, times(1)).acknowledge();
    }
    
    @Test
    public void testProcessBatchRecords() {
        // 准备批量测试数据
        ConsumerRecord<String, String> record1 = createConsumerRecord("key1", "value1");
        ConsumerRecord<String, String> record2 = createConsumerRecord("key2", "value2");
        List<ConsumerRecord<String, String>> records = Arrays.asList(record1, record2);
        
        // 执行测试
        listener.processBatchRecords(records, mockAck);
        
        // 验证结果
        assertTrue(listener.isBeforeBatchProcessCalled());
        assertTrue(listener.isProcessMessagesCalled());
        assertTrue(listener.isAfterBatchProcessCalled());
        assertEquals(2, listener.getProcessedBatchSize());
        
        // 验证确认
        verify(mockAck, times(1)).acknowledge();
    }
    
    @Test
    public void testExtractHeaders() {
        // 执行测试
        Map<String, String> headers = listener.extractHeaders(mockRecord);
        
        // 验证结果
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertEquals("test-trace-id", headers.get("X-Trace-Id"));
        assertEquals("test-value", headers.get("X-Test-Header"));
    }
    
    @Test
    public void testExtractTraceId() {
        // 执行测试
        String traceId = listener.extractTraceId(mockRecord);
        
        // 验证结果
        assertEquals("test-trace-id", traceId);
    }
    
    @Test
    public void testSendToDeadLetterQueue() {
        // 准备测试数据
        Exception testException = new RuntimeException("测试异常");
        
        // 模拟CompletableFuture
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(mockKafkaOperations.send(anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(future);
        
        // 执行测试
        listener.sendToDeadLetterQueue(mockRecord, testException);
        
        // 验证结果
        verify(mockKafkaOperations, times(1)).send(
                eq("test-topic.dlq"), // 死信队列名
                eq("test-key"),       // 原始消息键
                eq("test-value"),     // 原始消息值
                argThat(headers -> {  // 验证头信息包含必要字段
                    return headers.containsKey("X-Original-Topic") &&
                           headers.containsKey("X-Error-Message") &&
                           headers.containsKey("X-Retry-Count") &&
                           headers.containsKey("X-Trace-Id");
                })
        );
    }
    
    private ConsumerRecord<String, String> createConsumerRecord(String key, String value) {
        ConsumerRecord<String, String> record = Mockito.mock(ConsumerRecord.class);
        when(record.topic()).thenReturn("test-topic");
        when(record.partition()).thenReturn(0);
        when(record.offset()).thenReturn(100L);
        when(record.key()).thenReturn(key);
        when(record.value()).thenReturn(value);
        when(record.headers()).thenReturn(testHeaders);
        return record;
    }
    
    /**
     * 用于测试的监听器实现
     */
    private static class TestKafkaListener extends AbstractKafkaListener {
        private boolean beforeProcessCalled = false;
        private boolean processMessageCalled = false;
        private boolean afterProcessCalled = false;
        private boolean beforeBatchProcessCalled = false;
        private boolean processMessagesCalled = false;
        private boolean afterBatchProcessCalled = false;
        private boolean errorHandled = false;
        private String processedKey;
        private String processedValue;
        private int processedBatchSize;
        private boolean shouldThrowException = false;
        private int retryCount = 0;
        private KafkaOperations kafkaOperations;
        
        public void setKafkaOperations(KafkaOperations kafkaOperations) {
            this.kafkaOperations = kafkaOperations;
        }
        
        public void setShouldThrowException(boolean shouldThrowException) {
            this.shouldThrowException = shouldThrowException;
        }
        
        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }
        
        @Override
        protected void beforeProcess(ConsumerRecord<String, String> record) {
            beforeProcessCalled = true;
        }
        
        @Override
        protected void processMessage(String key, String value, ConsumerRecord<String, String> record) {
            processMessageCalled = true;
            processedKey = key;
            processedValue = value;
            
            if (shouldThrowException) {
                throw new KafkaException("测试异常");
            }
        }
        
        @Override
        protected void afterProcess(ConsumerRecord<String, String> record) {
            afterProcessCalled = true;
        }
        
        @Override
        protected void beforeBatchProcess(List<ConsumerRecord<String, String>> records) {
            beforeBatchProcessCalled = true;
        }
        
        @Override
        protected void processMessages(List<ConsumerRecord<String, String>> records) {
            processMessagesCalled = true;
            processedBatchSize = records.size();
        }
        
        @Override
        protected void afterBatchProcess(List<ConsumerRecord<String, String>> records) {
            afterBatchProcessCalled = true;
        }
        
        @Override
        protected void onError(ConsumerRecord<String, String> record, Exception e) {
            errorHandled = true;
        }
        
        @Override
        protected int getRetryCount(ConsumerRecord<String, String> record) {
            return retryCount;
        }
        
        @Override
        protected int getMaxRetries() {
            return 2; // 最大重试次数为2
        }

        // 用于测试的方法
        public KafkaOperations getKafkaOperations() {
            return this.kafkaOperations;
        }
        
        // Getters for verification
        public boolean isBeforeProcessCalled() {
            return beforeProcessCalled;
        }
        
        public boolean isProcessMessageCalled() {
            return processMessageCalled;
        }
        
        public boolean isAfterProcessCalled() {
            return afterProcessCalled;
        }
        
        public boolean isBeforeBatchProcessCalled() {
            return beforeBatchProcessCalled;
        }
        
        public boolean isProcessMessagesCalled() {
            return processMessagesCalled;
        }
        
        public boolean isAfterBatchProcessCalled() {
            return afterBatchProcessCalled;
        }
        
        public boolean isErrorHandled() {
            return errorHandled;
        }
        
        public String getProcessedKey() {
            return processedKey;
        }
        
        public String getProcessedValue() {
            return processedValue;
        }
        
        public int getProcessedBatchSize() {
            return processedBatchSize;
        }
    }
} 