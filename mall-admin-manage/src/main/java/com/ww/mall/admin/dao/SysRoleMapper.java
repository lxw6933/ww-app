package com.ww.mall.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.admin.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 删除角色下所有用户关联信息
     *
     * @param roleId roleId
     */
    void deleteRoleOfUser(Long roleId);

    /**
     * 删除角色下所有菜单关联信息
     *
     * @param roleId roleId
     */
    void deleteRoleOfMenu(Long roleId);

}

