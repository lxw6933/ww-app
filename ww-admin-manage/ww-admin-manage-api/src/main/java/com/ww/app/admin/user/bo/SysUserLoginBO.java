package com.ww.app.admin.user.bo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author ww
 * @create 2024-09-11- 09:29
 * @description:
 */
@Data
public class SysUserLoginBO {

    @NotEmpty(message = "账号不能为空")
    private String username;

    @NotEmpty(message = "密码不能为空")
    private String password;

}
