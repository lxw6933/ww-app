package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysPrintTemplateEntity;
import com.ww.mall.mvc.view.form.admin.SysPrintTemplateForm;
import com.ww.mall.mvc.view.vo.admin.SysPrintTemplateVO;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:24
 */
public interface SysPrintTemplateService extends IService<SysPrintTemplateEntity> {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    PageInfo<SysPrintTemplateVO> page(Pagination pagination, QueryWrapper<SysPrintTemplateEntity> query);

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysPrintTemplateVO
     */
    SysPrintTemplateVO info(Long id);

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysPrintTemplateVO
     */
    SysPrintTemplateVO save(SysPrintTemplateForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     * @return SysPrintTemplateVO
     */
    SysPrintTemplateVO update(SysPrintTemplateForm form);

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
