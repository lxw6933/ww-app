package com.ww.mall.member.interceptor;

import com.ww.mall.member.constant.CartConstant;
import com.ww.mall.member.to.UserInfoTo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * @description:
 * @author: ww
 * @create: 2021/7/3 下午8:58
 **/
@Component
public class CartInterceptor implements HandlerInterceptor {

    /**
     * 同一个线程共享变量信息
     */
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 执行controller之前执行拦截
     *
     * @param request request
     * @param response response
     * @param handler handler
     * @return boolean : true 放行 false： 拦截
     * @throws Exception e
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取用户是否登录
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session = request.getSession();
        if (session.getAttribute("") != null) {
            // 已登录
            userInfoTo.setUserId(1L);
        }
        Cookie[] cookies = request.getCookies();

        if (ArrayUtils.isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                if (CartConstant.TEMP_USER_COOKIE_NAME.equalsIgnoreCase(cookie.getName())) {
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }
        // 如果没有临时用户则创建一个保存在cookie里
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            userInfoTo.setUserKey(UUID.randomUUID().toString());
        }
        threadLocal.set(userInfoTo);
        return true;
    }

    /**
     * 执行完controller之后
     *
     * @param request request
     * @param response response
     * @param handler handler
     * @param modelAndView modelanView
     * @throws Exception e
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();
        if (!userInfoTo.isTempUser()) {
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_EXPIRE);
            response.addCookie(cookie);
        }
    }
}
