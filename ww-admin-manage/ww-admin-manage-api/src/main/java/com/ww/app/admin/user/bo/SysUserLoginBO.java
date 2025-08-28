package com.ww.app.admin.user.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author ww
 * @create 2024-09-11- 09:29
 * @description:
 */
@Data
@Schema(description = "后台登录BO")
public class SysUserLoginBO {

    @NotEmpty(message = "账号不能为空")
    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotEmpty(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotEmpty(message = "验证码不能为空")
    @Schema(description = "验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captcha;

    @NotEmpty(message = "验证码uuid不能为空")
    @Schema(description = "验证码uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uuid;

}
