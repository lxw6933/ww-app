package com.ww.mall.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ww.mall.admin.entity.SysUser;
import com.ww.mall.admin.entity.mongo.OperateLog;
import com.ww.mall.admin.service.OperateLogService;
import com.ww.mall.admin.service.SysUserService;
import com.ww.mall.admin.view.dto.OperateLogDTO;
import com.ww.mall.admin.view.query.SysOperateLogPageQuery;
import com.ww.mall.admin.view.vo.OperateLogVO;
import com.ww.mall.common.common.MallPageResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

/**
 * @author ww
 * @create 2024-09-19- 09:27
 * @description:
 */
@Slf4j
@Service
public class OperateLogServiceImpl implements OperateLogService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SysUserService sysUserService;

    @Async
    @Override
    public void save(OperateLogDTO operateLogDTO) {
        log.info("保存操作日志traceId【{}】", operateLogDTO.getTraceId());
        OperateLog operateLog = BeanUtil.toBean(operateLogDTO, OperateLog.class);
        mongoTemplate.save(operateLog);
    }

    @Override
    public MallPageResult<OperateLogVO> page(SysOperateLogPageQuery query) {
        String collectionName = "sys_operate_log";
        // number str sort
        Collation collation = Collation.of(Locale.CHINESE).numericOrdering(true);
        // query condition
        AggregationOperation matchAggregation = Aggregation.match(query.buildQuery());
        // sort condition
        AggregationOperation sortAggregation = Aggregation.sort(query.buildSort());
        // page condition
        AggregationOperation skip = Aggregation.skip((long) (query.getPageNum() - 1) * query.getPageSize());
        AggregationOperation limit = Aggregation.limit(query.getPageSize());
        // build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(matchAggregation, sortAggregation, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().collation(collation).build());
        // query aggregation data result
        AggregationResults<OperateLog> operateLogAggregationResult = mongoTemplate.aggregate(aggregation, collectionName, OperateLog.class);
        List<OperateLog> operateLogResultList = operateLogAggregationResult.getMappedResults();
        // totalCount
        Aggregation countAggregation = Aggregation.newAggregation(matchAggregation, Aggregation.count().as("totalCount"));

        AggregationResults<Document> countResults = mongoTemplate.aggregate(countAggregation, collectionName, Document.class);
        int total = !countResults.getMappedResults().isEmpty() ? countResults.getMappedResults().get(0).getInteger("totalCount") : 0;
        // return
        return new MallPageResult<>(query.getPageNum(), query.getPageSize(), total, operateLogResultList, operateLog -> {
            OperateLogVO vo = new OperateLogVO();
            BeanUtils.copyProperties(operateLog, vo);
            SysUser sysUser = sysUserService.getById(vo.getUserId());
            vo.setNickName(sysUser.getRealName());
            return vo;
        });
    }

}
