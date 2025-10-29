package com.ww.app.disruptor.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Disruptor配置属性
 *
 * @author ww-framework
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "ww.disruptor")
public class DisruptorProperties {

    /**
     * 是否启用Disruptor
     */
    private boolean enabled = true;

    /**
     * RingBuffer大小（必须是2的幂）
     */
    private int ringBufferSize = 1024;

    /**
     * 消费者线程数
     */
    private int consumerThreads = 4;

    /**
     * 生产者线程数
     */
    private int producerThreads = 2;

    /**
     * 批处理大小
     */
    private int batchSize = 100;

    /**
     * 批处理超时时间（毫秒）
     */
    private long batchTimeout = 1000L;

    /**
     * 等待策略: BLOCKING, YIELDING, SLEEPING, BUSY_SPIN
     */
    private String waitStrategy = "BLOCKING";

    /**
     * 是否启用批量处理
     */
    private boolean batchEnabled = true;

    /**
     * 持久化配置
     */
    private Persistence persistence = new Persistence();

    /**
     * 监控配置
     */
    private Metrics metrics = new Metrics();

    /**
     * 持久化配置
     */
    @Data
    public static class Persistence {
        /**
         * 是否启用持久化
         */
        private boolean enabled = false;

        /**
         * 持久化类型: file, redis, kafka, database
         */
        private String type = "file";

        /**
         * 数据目录
         */
        private String dataDir = "./data/disruptor";

        /**
         * 段文件大小（字节）
         */
        private long segmentSize = 67108864L; // 64MB

        /**
         * 段保留时间（小时）
         */
        private int retentionHours = 24;

        /**
         * 刷盘间隔（毫秒）
         */
        private long flushInterval = 1000L;

        /**
         * 是否同步刷盘
         */
        private boolean syncFlush = false;

    }

    /**
     * 监控配置
     */
    @Data
    public static class Metrics {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 指标前缀
         */
        private String prefix = "ww.disruptor";

        /**
         * 是否启用详细指标
         */
        private boolean detailed = false;

        /**
         * 是否启用链路追踪
         */
        private boolean tracing = false;

    }
}
