package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysLogEntity;
import com.ww.mall.mvc.view.query.admin.SysLogQuery;
import com.ww.mall.mvc.view.vo.admin.SysLogVO;

import java.io.ByteArrayOutputStream;

/**
 * @description: 系统日志
 * @author: ww
 * @create: 2021-05-13 09:21
 */
public interface SysLogService extends IService<SysLogEntity> {

    /**
     * 获取系统日志列表
     * @param pagination 分页条件
     * @param query 系统日志查询条件参数
     * @return PageInfo<SysLogVO>
     */
    PageInfo<SysLogVO> page(Pagination pagination, SysLogQuery query);

    /**
     * 获取指定系统日志
     * @param id 系统日志id
     * @return SysLogVO
     */
    SysLogVO info(Long id);

    /**
     * 系统日志下载
     * @param query 系统日志查询条件参数
     * @return ByteArrayOutputStream
     */
    ByteArrayOutputStream exportExcel(SysLogQuery query);
}
