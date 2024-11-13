package com.ww.mall.auth.entity;

import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import com.ww.mall.common.enums.LoginType;
import com.ww.mall.common.enums.UserType;
import com.ww.mall.common.thread.ThreadMdcUtil;
import com.ww.mall.common.utils.HttpContextUtils;
import com.ww.mall.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ww
 * @create 2024-09-22 12:58
 * @description:
 */
@Data
@Document("sys_login_log")
@EqualsAndHashCode(callSuper = true)
public class LoginLog extends BaseDoc {

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 登录用户id
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 用户类型
     */
    private UserType userType;

    /**
     * 登录类型
     */
    private LoginType loginType;

    /**
     * 是否登录成功
     */
    private Boolean loginSuccess;

    /**
     * 登录失败原因
     */
    private String loginMsg;

    /**
     * 登录ip
     */
    private String ip;

    /**
     * 登录方式
     */
    private String userAgent;

    public static LoginLog build(Long userId, UserType userType, LoginType loginType) {
        LoginLog loginLog = new LoginLog();
        loginLog.setTraceId(ThreadMdcUtil.getTraceId());
        loginLog.setUserId(userId);
        loginLog.setUserType(userType);
        loginLog.setLoginType(loginType);
        loginLog.setLoginSuccess(true);
        loginLog.setLoginMsg("登录成功");
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        loginLog.setIp(ServletUtil.getClientIP(request));
        loginLog.setUserAgent(request.getHeader(Header.USER_AGENT.toString()));
        return loginLog;
    }

    public static LoginLog build(String account, UserType userType, LoginType loginType, String loginResult) {
        LoginLog loginLog = new LoginLog();
        loginLog.setTraceId(ThreadMdcUtil.getTraceId());
        loginLog.setAccount(account);
        loginLog.setUserType(userType);
        loginLog.setLoginType(loginType);
        loginLog.setLoginSuccess(false);
        loginLog.setLoginMsg(loginResult);
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        loginLog.setIp(ServletUtil.getClientIP(request));
        loginLog.setUserAgent(request.getHeader(Header.USER_AGENT.toString()));
        return loginLog;
    }

}
