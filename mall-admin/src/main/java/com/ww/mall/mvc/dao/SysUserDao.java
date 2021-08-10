package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysUserEntity;
import com.ww.mall.mvc.view.form.admin.UserAndRoleForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import com.ww.mall.mvc.view.vo.admin.SysRoleVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 后台用户表
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Mapper
public interface SysUserDao extends BaseMapper<SysUserEntity> {

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
     * 插入用户角色中间表
     *
     * @param form 用户角色关联表单
     */
    void insertUserAndRole(UserAndRoleForm form);

    /**
     * 删除用户角色中间表记录
     *
     * @param userId 用户id
     */
    void removeUserAndRole(Long userId);

}
