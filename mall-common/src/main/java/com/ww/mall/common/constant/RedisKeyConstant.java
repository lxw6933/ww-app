package com.ww.mall.common.constant;

import java.io.Serializable;

/**
 * @description: redis key
 * @author: ww
 * @create: 2021-05-17 15:28
 */
public class RedisKeyConstant {

    private RedisKeyConstant() {}

    public static final String SIGN_LOCK_PREFIX = "sign_lock:";

    public static final String KEY_PREFIX  = "";
    public static final String SPLIT  = ":";

    /**
     * 缓存key的前缀
     */
    private static final String SESSION_KEY = "shiroSession:";

    /** 登录用户 */
    public static final String LOGIN_USER_KEY = "loginUser:";

    /** 后台用户 */
    public static final String USER_KEY = "sysUser:";

    /** 菜单 */
    public static final String MENU_KEY = "menu:";

    /** 字典 */
    public static final String DICT_KEY = "dict:";

    /** 配置 */
    public static final String CONFIG_KEY = "config:";

    /** app注册用户key*/
    public static final String APP_USER_COUNT_KEY = "appUserCount:";

    public static String getSessionKey(Serializable serializable) {
        return KEY_PREFIX + SESSION_KEY + serializable.toString();
    }

    public static String getLoginUserKey(Long userId) {
        return KEY_PREFIX + LOGIN_USER_KEY + userId;
    }

    public static String getUserKey(Long userId) {
        return KEY_PREFIX + USER_KEY + userId;
    }

    public static String getMenuKey(Integer type) {
        return KEY_PREFIX + MENU_KEY + type;
    }

    /**
     * key：username:info  用户信息
     */
    public static final String INFO = ":info";
    /**
     * key：uid:roles      用户角色信息
     */
    public static final String ROLES = ":roles";
    /**
     * key：uid:permission 用户权限信息
     */
    public static final String PERMISSIONS = ":permissions";
    /**
     * 所有权限信息集合
     */
    public static final String ALL_PERMISSIONS = "all_permissions";
    /**
     * 所有后台菜单集合(一级权限（子集合包含其他权限集合）)
     */
    public static final String ALL_PERMISSIONS_MENU = "all_permissions_menu";
    /**
     * 所有权限url集合
     */
    public static final String ALL_PERMISSIONS_URL = "all_permissions_url";
    /**
     * 所有文章分类集合
     */
    public static final String ALL_ARTICLE_CHANNELS = "article_channels";
    /**
     * 所有角色集合
     */
    public static final String ALL_ROLES = "all_roles";
    /**
     * 获取所有后台通知记录
     */
    public static final String ALL_ADMIN_NOTIFIES = "all_admin_notifies";
    /**
     * 文章点赞SET
     */
    public static final String ARTICLE_LIKE = "article_like:";
    /**
     * 文章收藏SET
     */
    public static final String ARTICLE_COLLECTION = "article_collection:";
    /**
     * 文章评论SET
     */
    public static final String ARTICLE_COMMENT = "article_comment:";

}
