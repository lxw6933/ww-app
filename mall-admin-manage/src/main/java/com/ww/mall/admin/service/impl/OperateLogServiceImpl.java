package com.ww.mall.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ww.mall.admin.entity.SysUser;
import com.ww.mall.admin.entity.mongo.OperateLog;
import com.ww.mall.admin.service.OperateLogService;
import com.ww.mall.admin.service.SysUserService;
import com.ww.mall.admin.view.dto.OperateLogDTO;
import com.ww.mall.admin.view.query.SysOperateLogMongoPage;
import com.ww.mall.admin.view.vo.OperateLogVO;
import com.ww.mall.common.common.MallPageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
        log.info("保存操作日志traceId[{}]", operateLogDTO.getTraceId());
        OperateLog operateLog = BeanUtil.toBean(operateLogDTO, OperateLog.class);
        mongoTemplate.save(operateLog);
    }

    @Override
    public MallPageResult<OperateLogVO> page(SysOperateLogMongoPage query) {
        // query aggregation data result
        List<OperateLog> operateLogResultList = query.buildPageQueryResult(mongoTemplate, OperateLog.class);
        // query aggregation data result totalCount
        int total = (int) query.buildPageQueryResultTotalCount(mongoTemplate, OperateLog.class);
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
