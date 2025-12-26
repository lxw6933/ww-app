package com.ww.app.open.processor;

import com.ww.app.common.interfaces.BulkDataHandler;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.open.entity.OpenApiCallLog;
import com.ww.app.open.entity.OpenApiStatistics;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Disruptor 批处理器：批量落库日志并更新统计（Mongo）
 */
@Slf4j
@Component
public class OpenApiLogBatchProcessor implements BatchEventProcessor<OpenApiCallLog> {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private BulkDataHandler<OpenApiCallLog> mongoBulkDataHandler;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    @Override
    public ProcessResult processBatch(EventBatch<OpenApiCallLog> batch) {
        List<Event<OpenApiCallLog>> events = batch.getEvents();
        if (events == null || events.isEmpty()) {
            return ProcessResult.success("empty batch");
        }

        List<OpenApiCallLog> logs = new ArrayList<>(events.size());
        Map<String, StatAgg> aggMap = new HashMap<>();

        for (Event<OpenApiCallLog> event : events) {
            OpenApiCallLog log = event.getPayload();
            if (log == null) {
                continue;
            }
            ensureTimes(log);
            logs.add(log);

            String statDate = DATE_FORMATTER.format(Instant.ofEpochMilli(log.getRequestTime()));
            String key = statDate + "#" + log.getAppCode() + "#" + log.getApiCode();

            StatAgg agg = aggMap.computeIfAbsent(key, k -> StatAgg.init(statDate, log.getAppCode(), log.getApiCode()));
            agg.add(log);
        }

        if (logs.isEmpty()) {
            return ProcessResult.success("no valid log");
        }

        // 批量插入日志
        mongoBulkDataHandler.bulkSave(logs);

        // 批量更新统计
        try {
            aggMap.values().forEach(this::upsertStat);
        } catch (Exception e) {
            log.error("批量更新OpenApiStatistics失败: {}", e.getMessage(), e);
            return ProcessResult.failure(e.getMessage());
        }

        return ProcessResult.success("processed " + logs.size());
    }

    private void upsertStat(StatAgg agg) {
        Query query = new Query(Criteria.where("statDate").is(agg.getStatDate())
                .and("appCode").is(agg.getAppCode())
                .and("apiCode").is(agg.getApiCode())
                .and("statType").is(0));

        Update update = new Update()
                .inc("totalCount", agg.getTotalCount())
                .inc("successCount", agg.getSuccessCount())
                .inc("failCount", agg.getFailCount())
                .inc("totalDuration", agg.getTotalDuration())
                .max("maxDuration", agg.getMaxDuration())
                .min("minDuration", agg.getMinDuration())
                .setOnInsert("statDate", agg.getStatDate())
                .setOnInsert("appCode", agg.getAppCode())
                .setOnInsert("apiCode", agg.getApiCode())
                .setOnInsert("statType", 0);

        mongoTemplate.upsert(query, update, OpenApiStatistics.class);
    }

    private void ensureTimes(OpenApiCallLog log) {
        long now = System.currentTimeMillis();
        if (log.getRequestTime() == null) {
            log.setRequestTime(now);
        }
        if (log.getResponseTime() == null) {
            log.setResponseTime(now);
        }
        if (log.getDuration() == null && log.getRequestTime() != null && log.getResponseTime() != null) {
            log.setDuration(log.getResponseTime() - log.getRequestTime());
        }
    }

    /**
     * 聚合统计
     */
    @Data
    @lombok.AllArgsConstructor
    private static class StatAgg {
        private final String statDate;
        private final String appCode;
        private final String apiCode;
        private long totalCount;
        private long successCount;
        private long failCount;
        private long totalDuration;
        private long maxDuration;
        private long minDuration;

        static StatAgg init(String statDate, String appCode, String apiCode) {
            return new StatAgg(statDate, appCode, apiCode, 0, 0, 0, 0, 0, Long.MAX_VALUE);
        }

        void add(OpenApiCallLog log) {
            long duration = log.getDuration() == null ? 0 : log.getDuration();
            this.totalCount++;
            if (log.getSuccess() != null && log.getSuccess() == 1) {
                this.successCount++;
            } else {
                this.failCount++;
            }
            this.totalDuration += duration;
            this.maxDuration = Math.max(this.maxDuration, duration);
            this.minDuration = Math.min(this.minDuration, duration);
        }
    }
}

