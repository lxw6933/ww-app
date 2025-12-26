package com.ww.app.open.service.impl;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.open.entity.OpenApiCallLog;
import com.ww.app.open.service.OpenApiCallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 开放平台API调用日志服务实现类（Disruptor + Mongo）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCallLogServiceImpl implements OpenApiCallLogService {

    private final DisruptorTemplate<OpenApiCallLog> openApiLogDisruptorTemplate;

    @Override
    public boolean saveCallLog(OpenApiCallLog callLog) {
        try {
            openApiLogDisruptorTemplate.publish("open-api-log", callLog);
            return true;
        } catch (Exception e) {
            log.error("发布API调用日志到Disruptor失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void saveCallLogAsync(OpenApiCallLog callLog) {
        saveCallLog(callLog);
    }
}

