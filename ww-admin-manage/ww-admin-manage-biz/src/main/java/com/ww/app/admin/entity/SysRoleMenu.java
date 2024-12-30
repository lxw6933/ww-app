package com.ww.app.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-20- 16:13
 * @description:
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 菜单id
     */
    private Long menuId;
}
