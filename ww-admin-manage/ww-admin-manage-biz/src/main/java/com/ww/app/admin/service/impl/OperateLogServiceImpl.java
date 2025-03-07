package com.ww.app.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ww.app.admin.entity.SysUser;
import com.ww.app.admin.entity.mongo.OperateLog;
import com.ww.app.admin.service.OperateLogService;
import com.ww.app.admin.service.SysUserService;
import com.ww.app.admin.view.dto.OperateLogDTO;
import com.ww.app.admin.view.query.SysOperateLogMongoPage;
import com.ww.app.admin.view.vo.OperateLogVO;
import com.ww.app.common.common.AppPageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
    public AppPageResult<OperateLogVO> page(SysOperateLogMongoPage query) {
        return query.buildPageResult(operateLog -> {
            OperateLogVO vo = new OperateLogVO();
            BeanUtils.copyProperties(operateLog, vo);
            SysUser sysUser = sysUserService.getById(vo.getUserId());
            vo.setNickName(sysUser.getRealName());
            return vo;
        });
    }

}
