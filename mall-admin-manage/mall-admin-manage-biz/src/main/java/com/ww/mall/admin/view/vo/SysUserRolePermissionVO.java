package com.ww.mall.admin.view.vo;

import com.ww.mall.admin.entity.SysMenu;
import com.ww.mall.admin.entity.SysRole;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-09-22 18:27
 * @description:
 */
@Data
public class SysUserRolePermissionVO {

    private List<SysRole> roleList;

    private List<SysMenu> menuList;

    public SysUserRolePermissionVO() {
        roleList = new ArrayList<>();
        menuList = new ArrayList<>();
    }

}
