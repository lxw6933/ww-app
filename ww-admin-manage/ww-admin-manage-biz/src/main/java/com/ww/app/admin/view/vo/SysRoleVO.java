package com.ww.app.admin.view.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2024-05-21- 14:19
 * @description:
 */
@Data
public class SysRoleVO {

    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编号
     */
    private String roleNo;

    /**
     * 状态
     */
    private Boolean status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 角色菜单id集合
     */
    private List<Long> permissionIds;

}
