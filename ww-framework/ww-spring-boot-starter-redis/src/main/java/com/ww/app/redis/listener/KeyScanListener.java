package com.ww.app.redis.listener;

/**
 * @author ww
 * @create 2025-10-16 15:45
 * @description:
 */
public interface KeyScanListener {

    void onKey(String key);

    void onFinish();

}
