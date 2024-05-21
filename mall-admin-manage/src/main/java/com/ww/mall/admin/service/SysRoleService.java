package com.ww.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.admin.view.form.SysRoleForm;
import com.ww.mall.admin.view.query.SysRolePageQuery;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.web.cmmon.MallPageResult;
import com.ww.mall.web.view.form.IdForm;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 分页
     *
     * @param query 查询条件
     * @return MallPageResult
     */
    MallPageResult<SysRoleVO> page(SysRolePageQuery query);

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysRoleVO
     */
    SysRoleVO info(Long id);

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysRoleVO
     */
    boolean save(SysRoleForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     * @return SysRoleVO
     */
    boolean update(SysRoleForm form);

    /**
     * 删除
     *
     * @param form form
     */
    boolean delete(IdForm form);

}

