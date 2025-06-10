package com.ww.app.lottery.domain.context;

import lombok.Data;

import java.util.Map;

/**
 * @author ww
 * @create 2025-06-06- 14:40
 * @description:
 */
@Data
public class LotteryContext {

    /** 用户ID */
    private Long userId;

    /** 活动编码 */
    private String activityCode;

    /** 其他参数 */
    private Map<String, Object> params;

}
