package com.ww.app.admin.view.form;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author ww
 * @create 2024-05-21- 10:43
 * @description:
 */
@Data
public class ModifyPasswordForm {

    /**
     * 旧密码
     */
    @NotEmpty(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotEmpty(message = "新密码不能为空")
    private String newPassword;

    /**
     * 再次确认新密码
     */
    @NotEmpty(message = "新密码不能为空")
    private String confirmNewPassword;

}
