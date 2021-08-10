package com.ww.mall.mvc.view.vo.admin;

import lombok.Data;

import java.util.Date;

/**
 * 后台角色表 - VO
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysRoleVO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色标识码 格式：ADMIN
     */
    private String roleNo;

    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 排序（默认10）
     */
    private Integer roleSort;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
