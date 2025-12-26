package com.ww.app.open.service;

import com.ww.app.open.entity.OpenApiStatistics;

/**
 * 开放平台API统计服务接口（Mongo）
 */
public interface OpenApiStatisticsService {

    /**
     * 更新API调用统计
     *
     * @param appCode  应用编码
     * @param apiCode  API编码
     * @param duration 响应耗时（毫秒）
     * @param success  是否成功
     */
    void updateStatistics(String appCode, String apiCode, Long duration, boolean success);

    /**
     * 获取API统计信息
     *
     * @param appCode  应用编码
     * @param apiCode  API编码
     * @param statDate 统计日期
     * @return 统计信息
     */
    OpenApiStatistics getStatistics(String appCode, String apiCode, String statDate);
}

