package com.ww.app.member.member.enums;

import com.ww.app.common.common.ResCode;

/**
 * @author ww
 * @create 2025-09-05 23:05
 * @description:
 */
public interface ErrorCodeConstants {

    // ========== 用户收件地址 1-004-004-000 ==========
    ResCode ADDRESS_NOT_EXISTS = new ResCode(1_004_004_000, "用户收件地址不存在");

    //========== 签到 1-004-010-000 ==========
    ResCode SIGN_IN_RECORD_TODAY_EXISTS = new ResCode(1_004_010_000, "今日已签到，请勿重复签到");
    ResCode NOT_RESIGN_FUTURE_DATE = new ResCode(1_004_010_001, "不能签到未来日期");
    ResCode RESIGN_DATE_NOT_PERIOD = new ResCode(1_004_010_002, "补签日期无效，仅允许补签当前周期内的过去日期");
    ResCode USE_UP_OF_RESIGN_COUNT = new ResCode(1_004_010_003, "当前周期补签次数已用完");

}
