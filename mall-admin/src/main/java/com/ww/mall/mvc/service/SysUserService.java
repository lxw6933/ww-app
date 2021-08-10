package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysUserEntity;
import com.ww.mall.mvc.view.form.admin.ModifyPasswordForm;
import com.ww.mall.mvc.view.form.admin.SysUserForm;
import com.ww.mall.mvc.view.form.admin.UserAndRoleForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import com.ww.mall.mvc.view.vo.admin.SysRoleVO;
import com.ww.mall.mvc.view.vo.admin.SysUserVO;

import java.util.List;


/**
 * 后台用户表 - service
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
public interface SysUserService extends IService<SysUserEntity> {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    PageInfo<SysUserVO> page(Pagination pagination, QueryWrapper<SysUserEntity> query);

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysUserVO
     */
    SysUserVO info(Long id);

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysUserVO
     */
    SysUserVO save(SysUserForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     * @return SysUserVO
     */
    SysUserVO update(SysUserForm form);

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
     * 通过用户username查询用户下所有的角色
     *
     * @param username 登录账号
     * @return List<SysRoleVO>
     */
    List<SysRoleVO> queryUserOfRole(String username);

    /**
     * 根据用户username查询用户拥有的所有权限
     *
     * @param username 登录账号
     * @return List<SysPermissionVO>
     */
    List<SysPermissionVO> queryUserOfPermission(String username);

    /**
     * 修改密码
     *
     * @param form 修改密码表单
     */
    void modifyPassword(ModifyPasswordForm form);

    /**
     * 插入用户角色中间表
     *
     * @param form 用户角色关联表单
     */
    void insertUserAndRole(UserAndRoleForm form);

}

