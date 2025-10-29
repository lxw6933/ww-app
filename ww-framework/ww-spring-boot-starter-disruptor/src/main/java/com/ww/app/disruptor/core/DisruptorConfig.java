package com.ww.app.disruptor.core;

import com.ww.app.disruptor.constans.DisruptorWaitStrategy;
import lombok.Data;

/**
 * Disruptor引擎配置类（已移除未使用的producerThreads参数）
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
     * 批处理大小
     */
    private int batchSize = 100;

    /**
     * 批处理超时时间（毫秒）
     */
    private long batchTimeout = 1000L;

    /**
     * 等待策略: {@link DisruptorWaitStrategy}
     */
    private String waitStrategy = DisruptorWaitStrategy.BLOCKING;

    /**
     * 是否启用批量处理
     */
    private boolean batchEnabled = true;

    /**
     * 验证配置
     */
    public void validate() {
        if (!isPowerOfTwo(ringBufferSize)) {
            throw new IllegalArgumentException("RingBuffer大小必须是2的幂，当前值: " + ringBufferSize);
        }

        if (consumerThreads <= 0) {
            throw new IllegalArgumentException("消费者线程数必须大于0，当前值: " + consumerThreads);
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("批处理大小必须大于0，当前值: " + batchSize);
        }

        if (batchTimeout <= 0) {
            throw new IllegalArgumentException("批处理超时必须大于0，当前值: " + batchTimeout);
        }
    }

    /**
     * 检查是否为2的幂
     */
    private boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
}
