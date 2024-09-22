package com.ww.mall.admin.view.vo;

import com.ww.mall.admin.enums.SysMenuType;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-21- 14:19
 * @description:
 */
@Data
public class SysMenuVO {

    private Long id;

    /**
     * 菜单类型
     */
    private SysMenuType type;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 父级编号
     */
    private Long pid;

    /**
     * URL地址
     */
    private String url;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 是否可见
     */
    private Boolean visible;

    /**
     * 是否有效
     */
    private Boolean valid;

}
