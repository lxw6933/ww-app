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
     * Redis 拼团数据兜底保留时长，单位秒。
     * <p>
     * 拼团过期或成功后仍保留两天，便于查询回放、补偿和排障。
     */
    public static final int REDIS_GROUP_DATA_RETAIN_SECONDS = 2 * 24 * 60 * 60;

    /**
     * 过期任务单次处理上限。
     */
    public static final int EXPIRE_JOB_BATCH_LIMIT = 100;

    /**
     * 拼团通知任务定时扫描间隔，单位毫秒。
     */
    public static final long GROUP_NOTIFY_TASK_FIXED_DELAY_MILLIS = 5_000L;

    /**
     * 拼团通知任务单次扫描上限。
     */
    public static final int GROUP_NOTIFY_TASK_BATCH_SIZE = 100;

    /**
     * 拼团通知任务“发送中”租约时长，单位毫秒。
     * <p>
     * 当实例在发送过程中崩溃时，其他实例可在租约过期后重新领取该任务。
     */
    public static final long GROUP_NOTIFY_TASK_SENDING_LEASE_MILLIS = 30_000L;

    /**
     * 拼团通知任务最大重试次数。
     */
    public static final int GROUP_NOTIFY_TASK_MAX_RETRY_COUNT = 5;

    private GroupBizConstants() {
    }
}
