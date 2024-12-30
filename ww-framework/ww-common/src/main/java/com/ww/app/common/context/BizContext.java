package com.ww.app.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-11-13- 10:27
 * @description:
 */
public class BizContext {

    private static final TransmittableThreadLocal<Map<String, Object>> context = new TransmittableThreadLocal<>();

    public static void set(String key, Object value) {
        if (context.get() == null) {
            context.set(new HashMap<>());
        }
        context.get().put(key, value);
    }

    public static Object get(String key) {
        return context.get() != null ? context.get().get(key) : null;
    }

    public static void clear() {
        if (context.get() != null) {
            context.get().clear();
        }
        context.remove();
    }

}
