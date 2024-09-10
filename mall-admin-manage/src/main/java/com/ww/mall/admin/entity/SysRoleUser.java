package com.ww.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-20- 16:14
 * @description:
 */
@Data
@TableName("sys_user_role")
public class SysRoleUser {

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 用户id
     */
    private Long userId;

}
