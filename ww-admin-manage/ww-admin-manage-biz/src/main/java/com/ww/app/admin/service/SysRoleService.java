package com.ww.app.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.app.admin.entity.SysRole;
import com.ww.app.admin.view.form.SysRoleForm;
import com.ww.app.admin.view.query.SysRolePageQuery;
import com.ww.app.admin.view.vo.SysRoleSelectVO;
import com.ww.app.admin.view.vo.SysRoleVO;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.common.IdForm;

import java.util.List;

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
    AppPageResult<SysRoleVO> page(SysRolePageQuery query);

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
     * @param idForm idForm
     */
    boolean delete(IdForm idForm);

    /**
     * 修改状态
     *
     * @param roleId 角色id
     * @return boolean
     */
    boolean modifyStatus(Long roleId);

    /**
     * 获取所有角色列表
     *
     * @return List<RoleSelectVO>
     */
    List<SysRoleSelectVO> getAllRole();

}

