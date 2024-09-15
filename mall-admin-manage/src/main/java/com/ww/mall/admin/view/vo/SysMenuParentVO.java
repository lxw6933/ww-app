package com.ww.mall.admin.view.vo;

import com.ww.mall.admin.enums.SysMenuType;
import lombok.Data;

/**
 * @author ww
 * @create 2024-09-15 13:01
 * @description:
 */
@Data
public class SysMenuParentVO {

    private Long id;

    /**
     * 菜单类型
     */
    private SysMenuType type;

    /**
     * 菜单名称
     */
    private String name;

}
