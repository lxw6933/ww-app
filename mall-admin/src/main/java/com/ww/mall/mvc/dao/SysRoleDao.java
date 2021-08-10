package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysRoleEntity;
import com.ww.mall.mvc.view.form.admin.RoleAndPermissionForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 后台角色表
 * 
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Mapper
public interface SysRoleDao extends BaseMapper<SysRoleEntity> {

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
