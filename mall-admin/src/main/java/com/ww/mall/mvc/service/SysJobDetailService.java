package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.mvc.entity.SysJobDetailEntity;
import com.ww.mall.mvc.entity.dto.SysJobDetailDTO;

import java.util.List;


/**
 * 任务详情 - service
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
public interface SysJobDetailService extends IService<SysJobDetailEntity> {

    /**
     * 获取任务详情记录
     * @param md5 md5
     * @return SysJobDetailEntity
     */
    SysJobDetailEntity getByMd5(String md5);

    /**
     * 获取可执行的定时任务
     * @return List<SysJobDetailEntity>
     */
    List<SysJobDetailDTO> listExecutionJob();

    /**
     * 获取停止的定时任务
     * @return List<SysJobDetailDTO>
     */
    List<SysJobDetailDTO> listStopJob();

}

