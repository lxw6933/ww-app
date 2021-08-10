package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysRoleEntity;
import com.ww.mall.mvc.view.form.admin.RoleAndPermissionForm;
import com.ww.mall.mvc.view.form.admin.SysRoleForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import com.ww.mall.mvc.view.vo.admin.SysRoleVO;

import java.util.List;


/**
 * 后台角色表 - service
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
public interface SysRoleService extends IService<SysRoleEntity> {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    PageInfo<SysRoleVO> page(Pagination pagination, QueryWrapper<SysRoleEntity> query);

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
    SysRoleVO save(SysRoleForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     * @return SysRoleVO
     */
    SysRoleVO update(SysRoleForm form);

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

    /**
     * 插入角色权限中间表
     *
     * @param form 角色权限表单
     */
    void insertRoleAndPermission(RoleAndPermissionForm form);

    /**
     * 删除角色权限中间表记录
     *
     * @param roleId 角色id
     */
    void removeRoleAndPermission(Long roleId);

    /**
     * 通过角色id查询角色下所有的权限
     *
     * @param roleId 角色id
     * @return List<SysPermissionVO>
     */
    List<SysPermissionVO> queryRoleOfPermission(Long roleId);

}

