package com.ww.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.admin.enums.SysMenuType;
import com.ww.mall.mybatis.common.BaseEntity;
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
     * 如果 url 为 http(s) 时，则它是外链
     */
    private String url;

    /**
     * 图标
     */
    private String icon;

    /**
     * 权限标识
     * 一般格式为：${系统}:${模块}:${操作}
     * - 对于后端，配合 @PreAuthorize 注解，配置 API 接口需要该权限，从而对 API 接口进行权限控制。
     * - 对于前端，配合前端标签，配置按钮是否展示，避免用户没有该权限时，结果可以看到该操作。
     */
    private String permission;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否可见【只有菜单使用】
     * 当设置为 true 时，该菜单不会展示在侧边栏，但是路由还是存在。
     */
    private Boolean visible;

    /**
     * 是否有效
     */
    private Boolean valid;

    public SysMenu() {
        this.pid = 0L;
        this.valid = true;
    }

}
