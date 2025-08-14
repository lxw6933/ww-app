package com.ww.app.common.utils;

import org.apache.skywalking.apm.toolkit.trace.TraceContext;

/**
 * @author ww
 * @create 2025-08-14 15:15
 * @description:
 */
public class TracerUtils {

    private TracerUtils() {}

    public static String getTraceId() {
        // 使用skywalking获取traceId
        return TraceContext.traceId();
    }

}
