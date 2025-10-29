package com.ww.app.disruptor.constans;

/**
 * @author ww
 * @create 2025-10-29 11:34
 * @description:
 * 选择指南：
 * 追求极致性能，不计成本： BusySpinWaitStrategy（并做好CPU绑定）。
 * 想要高性能，且对系统友好： YieldingWaitStrategy（这是大多数情况下的首选）。
 * 后台任务，不关心延迟，只希望省CPU： SleepingWaitStrategy 或 BlockingWaitStrategy。
 * 真的不知道选什么/传统应用： BlockingWaitStrategy。
 */
public class DisruptorWaitStrategy {

    private DisruptorWaitStrategy() {}

    /**
     * 最高的延迟、最节省 CPU
     * 适用场景：对性能（延迟和吞吐量）要求不高的场景，或者需要与使用传统锁的旧系统进行兼容。通常不推荐在追求性能的核心场景中使用。
     */
    public static final String BLOCKING = "BLOCKING";

    /**
     * 非常低的延迟、
     * 适用场景：需要非常低的延迟，但又不能接受 BusySpin 的 CPU 浪费。这是高性能场景中一个非常流行和推荐的折中方案
     */
    public static final String YIELDING = "YIELDING";

    /**
     * CPU 资源消耗极低、平均延迟和尾延迟 比 Yielding 和 BusySpin高得多
     * 适用场景：对延迟不敏感，但对 CPU 资源消耗非常在意的场景。或者系统中有大量不活跃的 Disruptor 实例，不希望它们占用 CPU。
     */
    public static final String SLEEPING = "SLEEPING";

    /**
     * 最低延迟、最高的 CPU 使用率
     * 适用场景：对延迟要求极其苛刻（例如，微秒级甚至纳秒级）
     */
    public static final String BUSY_SPIN = "BUSY_SPIN";

}
