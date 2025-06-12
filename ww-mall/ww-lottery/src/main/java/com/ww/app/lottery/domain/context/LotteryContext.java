package com.ww.app.lottery.domain.context;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2025-06-06- 14:40
 * @description:
 */
@Data
public class LotteryContext {

    public static final String ACTIVITY_KEY = "activity";

    /** 用户ID */
    private Long userId;

    /** 活动编码 */
    private String activityCode;

    /** 其他参数 */
    private Map<String, Object> params = new HashMap<>();

    public void buildParam(String key, Object value) {
        params.put(key, value);
    }

    public <T> T getParamValue(String key,  Class<T> tClass) {
        return tClass.cast(params.get(key));
    }

}
