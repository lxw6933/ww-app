package com.ww.app.admin.view.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author ww
 * @create 2024-05-21- 10:43
 * @description:
 */
@Data
@Schema(description = "修改密码表单")
public class ModifyPasswordForm {

    /**
     * 旧密码
     */
    @NotEmpty(message = "旧密码不能为空")
    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    /**
     * 新密码
     */
    @NotEmpty(message = "新密码不能为空")
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    /**
     * 再次确认新密码
     */
    @NotEmpty(message = "新密码不能为空")
    @Schema(description = "新密码确认", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmNewPassword;

}
