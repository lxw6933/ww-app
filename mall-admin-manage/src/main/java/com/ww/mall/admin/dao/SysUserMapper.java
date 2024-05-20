package com.ww.mall.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.admin.entity.SysUser;
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

    @Select("select role_id from sys_user_role where user_id = #{userId}")
    List<Long> findRoleIdsByUserId(Long userId);

    List<Long> findMenuIdsByRoleIds(List<Long> roleIds);

}

