package com.ww.app.auth.serivce;

import com.ww.app.admin.user.bo.SysUserLoginBO;
import com.ww.app.auth.view.vo.LoginResultVO;
import com.ww.app.member.member.bo.MemberLoginBO;

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
    LoginResultVO adminLogin(SysUserLoginBO sysUserLoginBO);

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
