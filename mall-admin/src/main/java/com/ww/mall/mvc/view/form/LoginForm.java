package com.ww.mall.mvc.view.form;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @description: 登录表单
 * @author: ww
 * @create: 2021/6/26 上午9:59
 **/
@Data
public class LoginForm {

    @NotEmpty(message = "账号不能为空")
    private String username;

    @NotEmpty(message = "密码不能为空")
    private String password;

}
