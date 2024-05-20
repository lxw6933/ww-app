package com.ww.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.admin.enums.SysMenuType;
import com.ww.mall.admin.enums.SysPlatformType;
import com.ww.mall.web.cmmon.BaseEntity;
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
     * 平台
     */
    private SysPlatformType platform;

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
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private Boolean status;

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 平台id
     */
    private Long platformId;

}
