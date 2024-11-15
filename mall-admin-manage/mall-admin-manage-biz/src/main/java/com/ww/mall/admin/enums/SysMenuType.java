package com.ww.mall.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-20- 09:55
 * @description:
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum SysMenuType {

    LEVEL_1_MENU("一级菜单"),
    LEVEL_2_MENU("二级菜单"),
    BUTTON("功能按钮"),
    ROUTE_BUTTON("路由按钮"),
    ROUTE_PAGE("路由页面");

    private String menuTypeName;

}
