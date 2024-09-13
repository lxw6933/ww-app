package com.ww.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.admin.entity.SysUser;
import com.ww.mall.admin.view.form.ModifyPasswordForm;
import com.ww.mall.admin.view.form.SysUserForm;
import com.ww.mall.admin.view.vo.CurrentSysUserInfoVO;
import com.ww.mall.web.view.bo.SysUserLoginBO;
import com.ww.mall.admin.view.query.SysUserPageQuery;
import com.ww.mall.admin.view.vo.SysMenuVO;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.admin.view.vo.SysUserVO;
import com.ww.mall.web.cmmon.MallPageResult;
import com.ww.mall.web.view.dto.SysUserDTO;
import com.ww.mall.web.view.form.IdForm;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户分页查询
     *
     * @param query query
     * @return page
     */
    MallPageResult<SysUserVO> page(SysUserPageQuery query);

    /**
     * 新增用户
     *
     * @param form form
     * @return boolean
     */
    boolean save(SysUserForm form);

    /**
     * 编辑用户
     *
     * @param form form
     * @return boolean
     */
    boolean update(SysUserForm form);

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return sysUserVO
     */
    SysUserVO info(Long userId);

    /**
     * 删除用户
     *
     * @param form form
     * @return boolean
     */
    boolean delete(IdForm form);

    /**
     * 修改用户密码
     *
     * @param modifyPasswordForm bo
     * @return boolean
     */
    boolean modifyPassword(ModifyPasswordForm modifyPasswordForm);

    /**
     * 重置用户密码
     *
     * @param userId 用户id
     * @return boolean
     */
    boolean resetPassword(Long userId);

    /**
     * 修改用户状态
     *
     * @param userId 修改状态用户
     * @return boolean
     */
    boolean modifySysUserStatus(Long userId, boolean status);

    /**
     * 账号密码获取用户信息
     *
     * @param username 账号
     * @param password 密码
     * @return sysUser
     */
    SysUserVO info(String username, String password);

    /**
     * 查询用户下所有的角色
     *
     * @param userId userId
     * @return List<SysRoleVO>
     */
    List<SysRoleVO> queryUserOfRole(Long userId);

    /**
     * 查询用户拥有的所有权限
     *
     * @param userId userId
     * @return List<SysMenuVO>
     */
    List<SysMenuVO> queryUserOfMenu(Long userId);

    /**
     * 后台登录
     *
     * @param form 登录form
     * @return SysUserDTO
     */
    SysUserDTO login(SysUserLoginBO form);

    /**
     * 获取登录者信息
     *
     * @return SysUserVO
     */
    CurrentSysUserInfoVO selfInfo();
}

