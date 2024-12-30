package com.ww.app.auth.view.form;

import com.ww.app.common.enums.SysPlatformType;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2024-05-21- 14:09
 * @description:
 */
@Data
public class LoginForm {

    @NotEmpty(message = "账号不能为空")
    private String username;

    @NotEmpty(message = "密码不能为空")
    private String password;

    @NotEmpty(message = "验证码不能为空")
    private String code;

    @NotNull(message = "登录平台不能为空")
    private SysPlatformType platform;

}
