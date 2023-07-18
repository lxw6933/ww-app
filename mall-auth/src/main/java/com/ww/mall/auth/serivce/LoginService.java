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
    LoginVO loginByVerityCode(MemberLoginBO memberLoginBO, HttpServletRequest request);

    void sendCode(String mobile);
}
