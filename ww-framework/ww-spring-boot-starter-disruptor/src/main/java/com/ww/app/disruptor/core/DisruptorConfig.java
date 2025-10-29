package com.ww.app.disruptor.core;

import lombok.Data;

/**
 * Disruptor引擎配置类
 *
 * @author ww-framework
 */
@Data
public class DisruptorConfig {

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
     * 等待策略
     */
    private String waitStrategy = "BLOCKING";

    /**
     * 是否启用批量处理
     */
    private boolean batchEnabled = true;

    public DisruptorConfig() {
    }

    /**
     * 验证RingBuffer大小是否为2的幂
     */
    public void validate() {
        if (!isPowerOfTwo(ringBufferSize)) {
            throw new IllegalArgumentException("RingBuffer大小必须是2的幂");
        }

        if (consumerThreads <= 0) {
            throw new IllegalArgumentException("消费者线程数必须大于0");
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("批处理大小必须大于0");
        }
    }

    /**
     * 检查是否为2的幂
     */
    private boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

}
