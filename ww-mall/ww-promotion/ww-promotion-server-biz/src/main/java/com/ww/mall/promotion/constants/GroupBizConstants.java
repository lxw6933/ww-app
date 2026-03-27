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
     * Redis 拼团终态缓存保留时长，单位秒。
     * <p>
     * 团成功、失败、售后关闭后，Redis 仍继续保留两天，便于查询回放、补偿和排障。
     */
    public static final int REDIS_GROUP_TERMINAL_RETAIN_SECONDS = 2 * 24 * 60 * 60;

    /**
     * 过期任务单次处理上限。
     */
    public static final int EXPIRE_JOB_BATCH_LIMIT = 100;

    /**
     * 过期任务单次调度最多连续拉取批次数。
     * <p>
     * 通过在一次调度窗口内连续处理多个批次，尽量降低到期高峰时“每次只扫一页”导致的积压。
     */
    public static final int EXPIRE_JOB_MAX_ROUNDS = 10;

    /**
     * 活动统计归档任务单次扫描上限。
     */
    public static final int ACTIVITY_STATS_SETTLE_BATCH_LIMIT = 100;

    /**
     * 补偿任务单次扫描上限。
     */
    public static final int COMPENSATION_JOB_BATCH_LIMIT = 100;

    /**
     * 补偿任务单次调度最多连续拉取批次数。
     */
    public static final int COMPENSATION_JOB_MAX_ROUNDS = 10;

    /**
     * 补偿任务首次重试延迟，单位毫秒。
     */
    public static final long COMPENSATION_INITIAL_DELAY_MILLIS = 30_000L;

    /**
     * 补偿任务最大重试次数。
     * <p>
     * 超过该次数后任务会从调度索引移除，并保留为 DEAD 状态，等待人工排查。
     */
    public static final int COMPENSATION_MAX_RETRY_COUNT = 20;

    /**
     * 补偿任务指数退避最大延迟，单位毫秒。
     */
    public static final long COMPENSATION_MAX_DELAY_MILLIS = 30 * 60_000L;

    private GroupBizConstants() {
    }
}
