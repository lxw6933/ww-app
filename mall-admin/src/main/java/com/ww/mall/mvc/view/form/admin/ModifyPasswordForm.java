package com.ww.mall.mvc.view.form.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @description: 修改密码表单
 * @author: ww
 * @create: 2021/6/27 上午12:37
 **/
@Data
public class ModifyPasswordForm {

    /**
     * 用户id
     */
    @NotNull(message = "id不能为空")
    private Long id;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

}
