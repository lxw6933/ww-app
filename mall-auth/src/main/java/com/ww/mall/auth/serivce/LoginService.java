package com.ww.mall.auth.serivce;

import com.ww.mall.auth.view.vo.AdminLoginResultVO;
import com.ww.mall.auth.view.vo.LoginResultVO;
import com.ww.mall.web.view.bo.MemberLoginBO;
import com.ww.mall.web.view.bo.SysUserLoginBO;

/**
 * @author ww
 * @create 2023-07-18- 10:36
 * @description:
 */
public interface LoginService {

    /**
     * 后台登录
     *
     * @param sysUserLoginBO bo
     * @return LoginVO
     */
    AdminLoginResultVO adminLogin(SysUserLoginBO sysUserLoginBO);

    /**
     * 验证码登录
     *
     * @param memberLoginBO bo
     * @return LoginVO
     */
    LoginResultVO clientMobileLogin(MemberLoginBO memberLoginBO);

    /**
     * 发送验证码
     *
     * @param mobile 手机号
     */
    void sendCode(String mobile);

}
