package com.ww.mall.web.holder;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * @author ww
 * @create 2024-12-23- 14:50
 * @description: openfeign 远程调用服务ip context
 */
public class ServerIpContextHolder {

    private static final ThreadLocal<String> SERVER_IP_THREAD_LOCAL = TransmittableThreadLocal.withInitial(() -> null);

    public static void set(String serverIp) {
        SERVER_IP_THREAD_LOCAL.set(serverIp);
    }

    public static String get() {
        return SERVER_IP_THREAD_LOCAL.get();
    }

    public static void clear() {
        SERVER_IP_THREAD_LOCAL.remove();
    }

}
