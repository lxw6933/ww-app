package com.ww.mall.promotion.constants;

/**
 * 拼团业务运行期常量。
 * <p>
 * 该类统一收敛可读性差的魔法值，便于后续按业务场景统一调参。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团运行期常量
 */
public final class GroupBizConstants {

    /**
     * 空订单信息 JSON。
     */
    public static final String EMPTY_ORDER_INFO_JSON = "{}";

    /**
     * 支付回调锁时长，单位秒。
     */
    public static final long PAY_CALLBACK_LOCK_SECONDS = 30L;

    /**
     * Redis 拼团数据兜底保留时长，单位秒。
     * <p>
     * 拼团过期或成功后仍保留两天，便于查询回放、补偿和排障。
     */
    public static final int REDIS_GROUP_DATA_RETAIN_SECONDS = 2 * 24 * 60 * 60;

    /**
     * Disruptor 处理器单批次大小。
     */
    public static final int EVENT_PROCESSOR_BATCH_SIZE = 100;

    /**
     * Disruptor 处理器批次超时时间，单位毫秒。
     */
    public static final long EVENT_PROCESSOR_BATCH_TIMEOUT_MILLIS = 200L;

    /**
     * 过期任务单次处理上限。
     */
    public static final int EXPIRE_JOB_BATCH_LIMIT = 100;

    /**
     * 同步任务单次处理上限。
     */
    public static final int SYNC_JOB_BATCH_LIMIT = 1000;

    private GroupBizConstants() {
    }
}
