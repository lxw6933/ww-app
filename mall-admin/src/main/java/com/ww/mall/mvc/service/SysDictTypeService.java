package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysDictTypeEntity;
import com.ww.mall.mvc.view.form.admin.SysDictTypeForm;
import com.ww.mall.mvc.view.vo.admin.SysDictTypeVO;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 17:47
 */
public interface SysDictTypeService extends IService<SysDictTypeEntity> {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    PageInfo<SysDictTypeVO> page(Pagination pagination, QueryWrapper<SysDictTypeEntity> query);

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysDictTypeVO
     */
    SysDictTypeVO info(Long id);

    /**
     * 新增
     *
     * @param form 表单信息
     */
    void save(SysDictTypeForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     */
    void update(SysDictTypeForm form);

    /**
     * 修改指定记录启用状态
     *
     * @param id 主键id
     */
    void updateStatus(Long id);

    /**
     * 批量删除
     *
     * @param ids 主键ID
     * @return BatchProcessingResult
     */
    BatchProcessingResult batchDelete(List<Long> ids);

}


