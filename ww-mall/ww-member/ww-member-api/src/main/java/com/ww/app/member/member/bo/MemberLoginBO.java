package com.ww.app.member.member.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * @author ww
 * @create 2023-07-18- 09:16
 * @description:
 */
@Data
public class MemberLoginBO {

    @NotBlank(message = "验证码不能为空")
    private String verifyCode;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1([3456789])\\d{9}$", message = "手机格式不正确")
    private String mobile;

}
