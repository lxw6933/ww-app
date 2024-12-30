package com.ww.app.admin.service;

import com.ww.app.admin.view.dto.OperateLogDTO;
import com.ww.app.admin.view.query.SysOperateLogMongoPage;
import com.ww.app.admin.view.vo.OperateLogVO;
import com.ww.app.common.common.AppPageResult;

/**
 * @author ww
 * @create 2024-09-19- 09:17
 * @description:
 */
public interface OperateLogService {

    /**
     * 记录操作日志
     *
     * @param operateLogDTO 创建请求
     */
    void save(OperateLogDTO operateLogDTO);

    /**
     * 获得操作日志分页列表
     *
     * @param query 分页条件
     * @return 操作日志分页列表
     */
    AppPageResult<OperateLogVO> page(SysOperateLogMongoPage query);

}
