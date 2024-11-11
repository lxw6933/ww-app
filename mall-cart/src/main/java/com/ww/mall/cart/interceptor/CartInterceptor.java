package com.ww.mall.cart.interceptor;

import com.ww.mall.cart.to.UserInfoTo;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.utils.AuthorizationContext;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/17 20:25
 **/
@Component
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> cartThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        UserInfoTo userInfoTo = new UserInfoTo();
        MallClientUser clientUser = AuthorizationContext.getClientUser(false);
        if (clientUser != null) {
            Long userId = clientUser.getId();
            // 用户登录
            userInfoTo.setUserId(userId);
        } else {
            // 临时用户
            Cookie[] cookies = request.getCookies();
            if (ArrayUtils.isNotEmpty(cookies)) {
                for (Cookie cookie : cookies) {
                    if (Constant.TEMP_USER_KEY.equals(cookie.getName())) {
                        userInfoTo.setTempUserKey(cookie.getValue());
                    }
                }
            }
            if (StringUtils.isEmpty(userInfoTo.getTempUserKey())) {
                userInfoTo.setTempUserKey(UUID.randomUUID().toString());
            }
        }
        cartThreadLocal.set(userInfoTo);
        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        UserInfoTo userInfoTo = cartThreadLocal.get();
        if (userInfoTo.getUserId() != null) {
            // 用户登录不刷新cookie有效期时长
            return;
        }
        Cookie cookie = new Cookie(Constant.TEMP_USER_KEY, userInfoTo.getTempUserKey());
        cookie.setMaxAge(Constant.TEMP_USER_COOKIE_TIMEOUT);
        response.addCookie(cookie);
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        cartThreadLocal.remove();
    }
}
