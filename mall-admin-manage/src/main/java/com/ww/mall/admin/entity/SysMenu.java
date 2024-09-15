package com.ww.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.admin.enums.SysMenuType;
import com.ww.mall.mybatisplus.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2024-05-20- 09:47
 * @description:
 */
@Data
@TableName("sys_menu")
@EqualsAndHashCode(callSuper = true)
public class SysMenu extends BaseEntity {

    /**
     * 父级编号
     */
    private Long pid;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单类型
     */
    private SysMenuType type;

    /**
     * URL地址
     */
    private String url;

    /**
     * 图标
     */
    private String icon;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否有效
     */
    private Boolean valid;

    public SysMenu() {
        this.pid = 0L;
        this.valid = true;
    }

}
