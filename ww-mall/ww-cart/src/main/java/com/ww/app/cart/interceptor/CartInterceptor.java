package com.ww.app.cart.interceptor;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.ww.app.cart.to.UserInfoTo;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.context.AuthorizationContext;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
//@Component
public class CartInterceptor implements HandlerInterceptor {

    public static TransmittableThreadLocal<UserInfoTo> cartThreadLocal = new TransmittableThreadLocal<>();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        UserInfoTo userInfoTo = new UserInfoTo();
        ClientUser clientUser = AuthorizationContext.getClientUser(false);
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
