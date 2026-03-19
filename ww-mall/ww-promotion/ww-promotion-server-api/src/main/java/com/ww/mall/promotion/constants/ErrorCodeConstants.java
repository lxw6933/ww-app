package com.ww.mall.promotion.constants;

import com.ww.app.common.common.ResCode;

/**
 * @author ww
 * @create 2025-12-11 16:43
 * @description:
 */
public interface ErrorCodeConstants {

    // ========== 拼团记录 1-013-011-000 ==========
    ResCode GROUP_RECORD_NOT_EXISTS = new ResCode(1_013_011_000, "拼团不存在");
    ResCode GROUP_RECORD_EXISTS = new ResCode(1_013_011_001, "拼团失败，已参与过该拼团");
    ResCode GROUP_RECORD_ERROR = new ResCode(1_013_011_002, "拼团异常");
    ResCode GROUP_RECORD_USER_FULL = new ResCode(1_013_011_003, "拼团失败，拼团人数已满");
    ResCode GROUP_RECORD_FAILED_HAVE_JOINED = new ResCode(1_013_011_004, "拼团失败，原因：存在该活动正在进行的拼团记录");
    ResCode GROUP_RECORD_FAILED_TIME_NOT_START = new ResCode(1_013_011_005, "拼团失败，活动未开始");
    ResCode GROUP_RECORD_FAILED_TIME_END = new ResCode(1_013_011_006, "拼团失败，活动已经结束");
    ResCode GROUP_CREATE_FAILED = new ResCode(1_013_011_007, "创建拼团失败");
    ResCode GROUP_RECORD_FAILED_TOTAL_LIMIT_COUNT_EXCEED = new ResCode(1_013_011_008, "拼团失败，原因：超出总购买次数");
    ResCode GROUP_RECORD_FAILED_ORDER_STATUS_UNPAID = new ResCode(1_013_011_009, "拼团失败，原因：存在未支付订单，请先支付");
    ResCode GROUP_RECORD_FAILED_DISABLE = new ResCode(1_013_011_010, "拼团失败，活动已禁用");
    ResCode GROUP_RECORD_ORDER_CODE_NOT_EXISTS = new ResCode(1_013_011_012, "拼团失败，订单信息不存在");
    ResCode GROUP_RECORD_ORDER_DUPLICATED = new ResCode(1_013_011_013, "拼团失败，订单已重复处理");
    ResCode GROUP_RECORD_SKU_NOT_SUPPORTED = new ResCode(1_013_011_014, "拼团失败，当前SKU不在活动范围内");
    ResCode GROUP_RECORD_AFTER_SALE_DUPLICATED = new ResCode(1_013_011_015, "拼团售后消息已处理");

    // ========== 拼团活动 1-013-012-000 ==========
    ResCode GROUP_ACTIVITY_PARAM_EMPTY = new ResCode(1_013_012_000, "拼团活动参数不能为空");
    ResCode GROUP_ACTIVITY_ID_EMPTY = new ResCode(1_013_012_001, "拼团活动ID不能为空");
    ResCode GROUP_ACTIVITY_NOT_EXISTS = new ResCode(1_013_012_002, "拼团活动不存在");
    ResCode GROUP_ACTIVITY_UPDATING_FORBIDDEN = new ResCode(1_013_012_003, "拼团活动进行中，不允许修改");
    ResCode GROUP_ACTIVITY_ENABLED_INVALID = new ResCode(1_013_012_004, "拼团活动启用状态参数错误");
    ResCode GROUP_ACTIVITY_START_END_INVALID = new ResCode(1_013_012_005, "拼团活动开始时间不能晚于结束时间");
    ResCode GROUP_ACTIVITY_REQUIRED_SIZE_INVALID = new ResCode(1_013_012_006, "拼团人数必须大于1");
    ResCode GROUP_ACTIVITY_EXPIRE_HOURS_INVALID = new ResCode(1_013_012_007, "拼团有效期必须大于0");
    ResCode GROUP_ACTIVITY_SKU_RULE_REQUIRED = new ResCode(1_013_012_008, "拼团活动至少需要配置一个可售SKU规则");

}
