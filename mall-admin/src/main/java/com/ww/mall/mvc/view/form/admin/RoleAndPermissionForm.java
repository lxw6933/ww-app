package com.ww.mall.mvc.view.form.admin;

import lombok.Data;

import java.util.List;

/**
 * @description: 角色权限关联表单
 * @author: ww
 * @create: 2021/6/27 上午5:24
 **/
@Data
public class RoleAndPermissionForm {

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 权限id集合
     */
    private List<Long> permissionIds;

}
