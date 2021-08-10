package com.ww.mall.utils;

import com.ww.mall.config.security.entity.MyUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/26 上午9:03
 **/
public class LoginUserUtils {

    private LoginUserUtils() {}

    /**
     * 获取当前登录用户名称
     *
     * @return User
     */
    public static String getCurrentUserName() {
        if (SecurityContextHolder.getContext() != null) {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return SecurityContextHolder.getContext().getAuthentication().getName();
            }
        }
        return "";
    }

    /**
     * 获取当前登录用户信息
     *
     * @return User
     */
    public static MyUserDetails getCurrentUser() {
        if (SecurityContextHolder.getContext() != null) {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() != null) {
                    return (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                }
            }
        }
        return null;
    }

}
