package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysPermissionEntity;
import com.ww.mall.mvc.view.form.admin.SysPermissionForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;

import java.util.List;


/**
 * 后台权限表 - service
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
public interface SysPermissionService extends IService<SysPermissionEntity> {

    /**
     * 分页
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    PageInfo<SysPermissionVO> page(Pagination pagination, QueryWrapper<SysPermissionEntity> query);

    /**
     * 详情
     * @param id 主键ID
     * @return SysPermissionVO
     */
    SysPermissionVO info(Long id);

    /**
     * 新增
     * @param form 表单信息
     * @return SysPermissionVO
     */
    SysPermissionVO save(SysPermissionForm form);

    /**
     * 编辑
     * @param form 表单信息
     */
    SysPermissionVO update(SysPermissionForm form);

    /**
     * 删除
     * @param id 主键ID
     */
    void deleteById(Long id);

    /**
     * 批量删除
     * @param ids 主键ID
     */
    BatchProcessingResult batchDelete(List<Long> ids);

}

