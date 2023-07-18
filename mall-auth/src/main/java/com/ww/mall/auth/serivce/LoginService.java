package com.ww.mall.auth.serivce;

import com.ww.mall.web.view.bo.MemberLoginBO;
import com.ww.mall.auth.vo.LoginVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ww
 * @create 2023-07-18- 10:36
 * @description:
 */
public interface LoginService {

    /**
     * 验证码登录
     *
     * @param memberLoginBO bo
     * @param request request
     * @return LoginVO
     */
    LoginVO loginByVerityCode(MemberLoginBO memberLoginBO, HttpServletRequest request);

    /**
     * 发送验证码
     *
     * @param mobile 手机号
     */
    void sendCode(String mobile);
}
