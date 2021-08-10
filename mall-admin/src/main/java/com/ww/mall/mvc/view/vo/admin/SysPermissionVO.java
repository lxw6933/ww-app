package com.ww.mall.mvc.view.vo.admin;

import lombok.Data;

import java.util.Date;

/**
 * 后台权限表 - VO
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysPermissionVO {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 父级权限 关联 sys_permission
     */
    private Long parentId;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限类型（0：目录；1：菜单；2：权限）
     */
    private Integer type;

    /**
     * 权限标识码 格式：xx:xx
     */
    private String code;

    /**
     * 权限url
     */
    private String url;

    /**
     * 权限api(多个url用,分隔)
     */
    private String permUrl;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序(默认10)
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
