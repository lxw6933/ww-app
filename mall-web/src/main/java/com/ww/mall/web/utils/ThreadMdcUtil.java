package com.ww.mall.web.utils;

import cn.hutool.core.util.IdUtil;
import com.ww.mall.common.constant.Constant;
import org.slf4j.MDC;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @description: 解决多线程丢失traceId问题
 * @author: ww
 * @create: 2023/7/8 11:30
 **/
public class ThreadMdcUtil {
    public static void setTraceIdIfAbsent() {
        if (MDC.get(Constant.TRACE_ID) == null) {
            MDC.put(Constant.TRACE_ID, IdUtil.objectId());
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
