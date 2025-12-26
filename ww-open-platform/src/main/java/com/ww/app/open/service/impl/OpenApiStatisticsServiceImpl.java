package com.ww.app.open.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.ww.app.open.entity.OpenApiStatistics;
import com.ww.app.open.service.OpenApiStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 开放平台API统计服务实现类（Mongo）
 */
@Slf4j
@Service
public class OpenApiStatisticsServiceImpl implements OpenApiStatisticsService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void updateStatistics(String appCode, String apiCode, Long duration, boolean success) {
        String statDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Query query = new Query();
        query.addCriteria(Criteria.where("statDate").is(statDate)
                .and("appCode").is(appCode)
                .and("apiCode").is(apiCode)
                .and("statType").is(0));

        Update update = new Update()
                .inc("totalCount", 1)
                .inc(success ? "successCount" : "failCount", 1)
                .inc("totalDuration", duration == null ? 0L : duration)
                .max("maxDuration", duration)
                .min("minDuration", duration)
                .setOnInsert("statDate", statDate)
                .setOnInsert("appCode", appCode)
                .setOnInsert("apiCode", apiCode)
                .setOnInsert("statType", 0);

        UpdateResult result = mongoTemplate.upsert(query, update, OpenApiStatistics.class);
        log.debug("更新统计 result={}, appCode={}, apiCode={}, duration={}", result.getMatchedCount(), appCode, apiCode, duration);
    }

    @Override
    public OpenApiStatistics getStatistics(String appCode, String apiCode, String statDate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("statDate").is(statDate)
                .and("appCode").is(appCode)
                .and("apiCode").is(apiCode)
                .and("statType").is(0));

        OpenApiStatistics statistics = mongoTemplate.findOne(query, OpenApiStatistics.class);
        if (statistics != null && statistics.getTotalCount() != null && statistics.getTotalCount() > 0) {
            long totalDuration = statistics.getTotalDuration() == null ? 0L : statistics.getTotalDuration();
            statistics.setAvgDuration(totalDuration / statistics.getTotalCount());
        }
        return statistics;
    }
}

