package com.ww.mall.mvc.view.form.admin;

import lombok.Data;

import java.util.List;

/**
 * @description: 用户角色关联表单
 * @author: ww
 * @create: 2021/6/27 上午5:24
 **/
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
