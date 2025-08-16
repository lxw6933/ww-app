package com.ww.app.common.enums;

/**
 * @author ww
 * @create 2025-08-14 23:10
 * @description: Web 过滤器顺序的枚举类，保证过滤器按照符合我们的预期
 */
public interface WebFilterOrderEnum {

    int CORS_FILTER = Integer.MIN_VALUE;

    int TRACE_FILTER = CORS_FILTER + 1;

}
