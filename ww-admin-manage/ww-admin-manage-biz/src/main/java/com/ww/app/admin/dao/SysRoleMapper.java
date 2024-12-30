package com.ww.app.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.app.admin.entity.SysRole;
import com.ww.app.admin.view.form.RoleAndMenuForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {


    /**
     * 插入角色权限中间表
     *
     * @param form 角色权限表单
     */
    void insertRoleAndPermission(RoleAndMenuForm form);

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
     * @return List<Long>
     */
    @Select("select menu_id from sys_role_menu where role_id = #{roleId}")
    List<Long> findMenuIdsByRoleId(Long roleId);

}

