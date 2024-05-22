package com.ww.mall.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.common.enums.SysPlatformType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询角色下拥有的所有dataId
     *
     * @param roleId 角色id
     * @param platform 平台类型
     * @return List
     */
    List<Long> queryRoleOfAllData(Long roleId, SysPlatformType platform);

    /**
     * 查询dataId下所有的角色
     *
     * @param dataId dataId
     * @param platform platform
     * @return List
     */
    List<Long> queryDataIdOfAllRoleId(Long dataId, SysPlatformType platform);

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

