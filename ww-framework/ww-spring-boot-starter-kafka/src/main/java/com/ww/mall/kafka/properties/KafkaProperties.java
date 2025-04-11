package com.ww.mall.kafka.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka属性配置类
 */
@ConfigurationProperties(prefix = "ww.kafka")
public class KafkaProperties {
    
    /**
     * 是否启用自定义Kafka组件
     */
    private boolean enabled = true;
    
    /**
     * 消息重试次数
     */
    private int retryTimes = 3;
    
    /**
     * 生产者消息发送超时时间（毫秒）
     */
    private long sendTimeout = 10000;
    
    /**
     * 是否启用消息压缩
     */
    private boolean compressionEnabled = true;
    
    /**
     * 消息压缩类型：none, gzip, snappy, lz4, zstd
     */
    private String compressionType = "snappy";
    
    /**
     * 是否启用事务
     */
    private boolean transactionEnabled = false;
    
    /**
     * 批量消费大小
     */
    private int batchSize = 100;
    
    /**
     * 消费者监听线程数
     */
    private int consumerConcurrency = 3;
    
    /**
     * 死信队列配置
     */
    private DeadLetter deadLetter = new DeadLetter();
    
    /**
     * 跟踪配置
     */
    private Tracing tracing = new Tracing();
    
    /**
     * 调度器配置
     */
    private Scheduler scheduler = new Scheduler();
    
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    
    /**
     * 死信队列配置
     */
    public static class DeadLetter {
        /**
         * 是否启用死信队列
         */
        private boolean enabled = true;
        
        /**
         * 死信队列主题后缀
         */
        private String suffix = ".dlq";
        
        /**
         * 是否自动创建死信主题
         */
        private boolean autoCreateTopics = false;
        
        /**
         * 死信队列保留时间（毫秒）
         */
        private long retentionMs = 604800000; // 默认7天
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getSuffix() {
            return suffix;
        }
        
        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }
        
        public boolean isAutoCreateTopics() {
            return autoCreateTopics;
        }
        
        public void setAutoCreateTopics(boolean autoCreateTopics) {
            this.autoCreateTopics = autoCreateTopics;
        }
        
        public long getRetentionMs() {
            return retentionMs;
        }
        
        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }
    }
    
    /**
     * 跟踪配置
     */
    public static class Tracing {
        /**
         * 是否启用跟踪
         */
        private boolean enabled = true;
        
        /**
         * 是否传播跟踪ID
         */
        private boolean propagateTraceId = true;
        
        /**
         * 跟踪ID的头信息名称
         */
        private String traceIdHeaderName = "X-Trace-Id";
        
        /**
         * 是否传递MDC上下文
         */
        private boolean propagateMdc = true;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isPropagateTraceId() {
            return propagateTraceId;
        }
        
        public void setPropagateTraceId(boolean propagateTraceId) {
            this.propagateTraceId = propagateTraceId;
        }
        
        public String getTraceIdHeaderName() {
            return traceIdHeaderName;
        }
        
        public void setTraceIdHeaderName(String traceIdHeaderName) {
            this.traceIdHeaderName = traceIdHeaderName;
        }
        
        public boolean isPropagateMdc() {
            return propagateMdc;
        }
        
        public void setPropagateMdc(boolean propagateMdc) {
            this.propagateMdc = propagateMdc;
        }
    }
    
    /**
     * 调度器配置
     */
    public static class Scheduler {
        /**
         * 线程池大小
         */
        private int poolSize = 5;
        
        /**
         * 是否为守护线程
         */
        private boolean daemon = true;
        
        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "kafka-delayed-sender-";
        
        /**
         * 队列容量
         */
        private int queueCapacity = 100;
        
        public int getPoolSize() {
            return poolSize;
        }
        
        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }
        
        public boolean isDaemon() {
            return daemon;
        }
        
        public void setDaemon(boolean daemon) {
            this.daemon = daemon;
        }
        
        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
        
        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
        
        public int getQueueCapacity() {
            return queueCapacity;
        }
        
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
    
    /**
     * 生产者配置
     */
    public static class Producer {
        /**
         * 幂等性
         */
        private boolean idempotenceEnabled = true;
        
        /**
         * 消息确认级别：0, 1, all
         */
        private String acks = "all";
        
        /**
         * 生产者缓冲大小
         */
        private int bufferMemory = 33554432;
        
        /**
         * 生产者拦截器类名
         */
        private String interceptor;
        
        public boolean isIdempotenceEnabled() {
            return idempotenceEnabled;
        }
        
        public void setIdempotenceEnabled(boolean idempotenceEnabled) {
            this.idempotenceEnabled = idempotenceEnabled;
        }
        
        public String getAcks() {
            return acks;
        }
        
        public void setAcks(String acks) {
            this.acks = acks;
        }
        
        public int getBufferMemory() {
            return bufferMemory;
        }
        
        public void setBufferMemory(int bufferMemory) {
            this.bufferMemory = bufferMemory;
        }
        
        public String getInterceptor() {
            return interceptor;
        }
        
        public void setInterceptor(String interceptor) {
            this.interceptor = interceptor;
        }
    }
    
    /**
     * 消费者配置
     */
    public static class Consumer {
        /**
         * 消费者组ID
         */
        private String groupId = "default-group";
        
        /**
         * 是否自动提交偏移量
         */
        private boolean autoCommit = false;
        
        /**
         * 消费者拦截器类名
         */
        private String interceptor;
        
        /**
         * 是否启用批量消费
         */
        private boolean batchEnabled = false;
        
        /**
         * 并发消费者数量
         */
        private int concurrency = 3;
        
        public String getGroupId() {
            return groupId;
        }
        
        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
        
        public boolean isAutoCommit() {
            return autoCommit;
        }
        
        public void setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
        }
        
        public String getInterceptor() {
            return interceptor;
        }
        
        public void setInterceptor(String interceptor) {
            this.interceptor = interceptor;
        }
        
        public boolean isBatchEnabled() {
            return batchEnabled;
        }
        
        public void setBatchEnabled(boolean batchEnabled) {
            this.batchEnabled = batchEnabled;
        }
        
        public int getConcurrency() {
            return concurrency;
        }
        
        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getRetryTimes() {
        return retryTimes;
    }
    
    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }
    
    public long getSendTimeout() {
        return sendTimeout;
    }
    
    public void setSendTimeout(long sendTimeout) {
        this.sendTimeout = sendTimeout;
    }
    
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
    
    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }
    
    public String getCompressionType() {
        return compressionType;
    }
    
    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }
    
    public boolean isTransactionEnabled() {
        return transactionEnabled;
    }
    
    public void setTransactionEnabled(boolean transactionEnabled) {
        this.transactionEnabled = transactionEnabled;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public int getConsumerConcurrency() {
        return consumerConcurrency;
    }
    
    public void setConsumerConcurrency(int consumerConcurrency) {
        this.consumerConcurrency = consumerConcurrency;
    }
    
    public Producer getProducer() {
        return producer;
    }
    
    public void setProducer(Producer producer) {
        this.producer = producer;
    }
    
    public Consumer getConsumer() {
        return consumer;
    }
    
    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
    
    public DeadLetter getDeadLetter() {
        return deadLetter;
    }
    
    public void setDeadLetter(DeadLetter deadLetter) {
        this.deadLetter = deadLetter;
    }
    
    public Tracing getTracing() {
        return tracing;
    }
    
    public void setTracing(Tracing tracing) {
        this.tracing = tracing;
    }
    
    public Scheduler getScheduler() {
        return scheduler;
    }
    
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
} 