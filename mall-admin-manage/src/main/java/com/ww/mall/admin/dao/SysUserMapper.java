package com.ww.mall.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.admin.entity.SysUser;
import com.ww.mall.admin.view.form.UserAndRoleForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询用户拥有的所有角色id
     *
     * @param userId userId
     * @return roleIdList
     */
    @Select("select role_id from sys_role_user where user_id = #{userId} and status = true")
    List<Long> findRoleIdsByUserId(Long userId);

    /**
     * 查询所有角色下所有权限
     *
     * @param roleIds roleIdList
     * @return 所有权限id
     */
    List<Long> findMenuIdsByRoleIds(List<Long> roleIds);

    /**
     * 保存用户角色信息
     *
     * @param form from
     */
    void addUserOfRoleInfo(UserAndRoleForm form);

    /**
     * 删除用户角色信息
     *
     * @param userId userId
     */
    void deleteUserOfRole(Long userId);
}

