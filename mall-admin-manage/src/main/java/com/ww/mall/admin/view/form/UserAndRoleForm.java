package com.ww.mall.admin.view.form;

import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-21- 13:53
 * @description:
 */
@Data
public class UserAndRoleForm {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 角色id集合
     */
    private List<Long> roleIds;

}
