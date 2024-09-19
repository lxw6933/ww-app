package com.ww.mall.admin.service;

import com.ww.mall.admin.view.dto.OperateLogDTO;
import com.ww.mall.admin.view.query.SysOperateLogPageQuery;
import com.ww.mall.admin.view.vo.OperateLogVO;
import com.ww.mall.common.common.MallPageResult;

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
    MallPageResult<OperateLogVO> page(SysOperateLogPageQuery query);

}
