package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 后台用户表 - Form
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysUserForm {

    /**
     * 用户ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 中心端ID
     */
    private Long centerId;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 账号状态（1：正常；0：冻结）
     */
    private Integer status;

    /**
     * 角色id集合
     */
    private List<Long> roleIds;

}
