package com.ww.mall.mvc.view.form.mini;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @description: 小程序认证表单
 * @author: ww
 * @create: 2021-06-15 11:05
 */
@Data
public class AuthForm {

    /**
     * 登录code
     */
    @NotEmpty(message = "登录code不能为空")
    private String code;

    /**
     * 用户基本信息json
     */
    private String userData;

    /**
     * 微信绑定手机号
     */
    private String mobile;

    /**
     * 加密数据（手机号码等）
     */
    String encryptedData;

    /**
     * 加密算法的初始向量
     */
    String iv;


}
