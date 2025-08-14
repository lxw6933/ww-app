package com.ww.app.common.thread;

import cn.hutool.core.util.IdUtil;
import com.ww.app.common.constant.Constant;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @description: 解决多线程丢失traceId问题
 * @author: ww
 * @create: 2023/7/8 11:30
 * @deprecated 使用skywalking agent
 **/
@Deprecated
public class ThreadMdcUtil {

    public static String getTraceId() {
        return MDC.get(Constant.TRACE_ID);
    }

    public static void removeTraceId() {
        MDC.remove(Constant.TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        MDC.put(Constant.TRACE_ID, traceId);
    }

    public static void setTraceIdIfAbsent() {
        if (getTraceId() == null) {
            setTraceId(IdUtil.objectId());
        }
    }

    public static <T> Callable<T> wrap(final Callable<T> callable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                return callable.call();
            } finally {
                MDC.clear();
            }
        };
    }

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
