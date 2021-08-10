package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.MqMsgLogEntity;
import com.ww.mall.mvc.view.form.admin.MqMsgLogForm;
import com.ww.mall.mvc.view.vo.admin.MqMsgLogVO;

import java.util.List;


/**
 * 消费日志 - service
 *
 * @author ww
 * @date 2021-06-30 09:30:37
 */
public interface MqMsgLogService extends IService<MqMsgLogEntity> {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    PageInfo<MqMsgLogVO> page(Pagination pagination, QueryWrapper<MqMsgLogEntity> query);

    /**
     * 详情
     *
     * @param id 主键ID
     * @return MqMsgLogVO
     */
    MqMsgLogVO info(Long id);

    /**
     * 新增
     *
     * @param form 表单信息
     * @return MqMsgLogVO
     */
    MqMsgLogVO save(MqMsgLogForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     * @return MqMsgLogVO
     */
    MqMsgLogVO update(MqMsgLogForm form);

    /**
     * 删除
     *
     * @param id 主键ID
     */
    void deleteById(Long id);

    /**
     * 批量删除
     *
     * @param ids 主键ID
     * @return BatchProcessingResult
     */
    BatchProcessingResult batchDelete(List<Long> ids);

}

