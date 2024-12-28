package com.ww.mall.admin.view.form;

import com.ww.mall.admin.enums.SysMenuType;
import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2024-05-21- 13:32
 * @description:
 */
@Data
public class SysMenuForm {

    /**
     * 用户ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
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

}
