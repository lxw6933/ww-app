package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysPrintTypeEntity;
import com.ww.mall.mvc.view.form.admin.SysPrintTypeForm;
import com.ww.mall.mvc.view.vo.admin.SysPrintTypeVO;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:42
 */
public interface SysPrintTypeService extends IService<SysPrintTypeEntity> {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    PageInfo<SysPrintTypeVO> page(Pagination pagination, QueryWrapper<SysPrintTypeEntity> query);

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysPrintTypeVO
     */
    SysPrintTypeVO info(Long id);

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysPrintTypeVO
     */
    SysPrintTypeVO save(SysPrintTypeForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     * @return SysPrintTypeVO
     */
    SysPrintTypeVO update(SysPrintTypeForm form);

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
