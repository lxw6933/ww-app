package com.ww.mall.admin.view.form;

import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2024-09-11- 14:41
 * @description:
 */
@Data
public class RoleAndMenuForm {

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 权限id集合
     */
    private List<Long> permissionIds;

}
