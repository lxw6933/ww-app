package com.ww.mall.promotion.constants;

/**
 * 拼团文案常量。
 * <p>
 * 该类仅承载复用频率较高、且需要跨类保持一致的提示文案，避免散落硬编码字符串。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团统一文案常量
 */
public final class GroupTextConstants {

    /**
     * 支付回调处理中。
     */
    public static final String PAY_CALLBACK_IN_PROGRESS = "支付回调处理中";

    /**
     * 用户信息不存在。
     */
    public static final String USER_INFO_MISSING = "用户信息不存在";

    /**
     * 开团回调缺少活动ID。
     */
    public static final String START_CALLBACK_ACTIVITY_ID_REQUIRED = "开团回调缺少活动ID";

    /**
     * 参团回调缺少拼团ID。
     */
    public static final String JOIN_CALLBACK_GROUP_ID_REQUIRED = "参团回调缺少拼团ID";

    /**
     * 幂等回放返回历史结果。
     */
    public static final String IDEMPOTENT_REPLAY_RETURNED = "幂等回放返回历史结果";

    /**
     * 当前服务不消费成功通知以避免消息自激回流。
     */
    public static final String MQ_SUCCESS_CALLBACK_SKIPPED = "当前服务不再消费成功通知回调内部状态，避免消息自激回流";

    /**
     * 当前服务不消费失败通知以避免消息自激回流。
     */
    public static final String MQ_FAILED_CALLBACK_SKIPPED = "当前服务不再消费失败通知回调内部状态，避免消息自激回流";

    /**
     * 当前仅记录退款待办。
     */
    public static final String REFUND_PENDING_RECORDED = "当前仅记录退款待办，尚未接入真实支付退款";

    private GroupTextConstants() {
    }
}
