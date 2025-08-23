package com.ww.app.operatelog.core.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.operatelog.core.entity.OperateLog;
import com.ww.app.operatelog.core.service.OperateLogService;
import com.ww.app.operatelog.view.dto.OperateLogDTO;
import com.ww.app.operatelog.view.query.SysOperateLogMongoPageQuery;
import com.ww.app.operatelog.view.vo.OperateLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Resource;
import java.util.function.Function;

/**
 * @author ww
 * @create 2024-09-19- 09:27
 * @description:
 */
@Slf4j
public class OperateLogServiceImpl implements OperateLogService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Async("operateLogTaskExecutor")
    @Override
    public void save(OperateLogDTO operateLogDTO) {
        log.info("保存操作日志traceId[{}]", operateLogDTO.getTraceId());
        OperateLog operateLog = BeanUtil.toBean(operateLogDTO, OperateLog.class);
        mongoTemplate.save(operateLog);
    }

    @Override
    public AppPageResult<OperateLogVO> page(SysOperateLogMongoPageQuery query, Function<Long, String> nameFun) {
        return query.buildPageConvertResult(operateLog -> {
            OperateLogVO vo = new OperateLogVO();
            BeanUtils.copyProperties(operateLog, vo);
            vo.setNickName(nameFun.apply(vo.getUserId()));
            return vo;
        });
    }

}
