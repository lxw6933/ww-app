package com.ww.app.open.service;

import com.ww.app.open.entity.OpenApiCallLog;

/**
 * 开放平台API调用日志服务接口（Mongo + Disruptor）
 */
public interface OpenApiCallLogService {

    /**
     * 发布日志到异步通道
     *
     * @param callLog 调用日志
     * @return 是否已提交
     */
    boolean saveCallLog(OpenApiCallLog callLog);

    /**
     * 兼容异步方法签名
     */
    void saveCallLogAsync(OpenApiCallLog callLog);
}

