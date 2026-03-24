package com.ww.app.ssh.model;

import lombok.Data;

/**
 * 中间件后台展示对象。
 * <p>
 * 用于向前端返回某个实例下配置的中间件后台入口、访问地址和测试账号密码。
 * 当前仅服务于测试环境的统一跳转场景，不参与任何自动登录逻辑。
 * </p>
 */
@Data
public class MiddlewareConsoleVO {

    /**
     * 中间件编码。
     */
    private String code;

    /**
     * 展示名称。
     */
    private String name;

    /**
     * 中间件后台访问地址。
     */
    private String url;

    /**
     * 当前地址是否允许通过系统统一跳转。
     * <p>
     * 该字段仅描述“打开后台”按钮是否可用，不会修改或纠正原始配置值，
     * 以便前端能够按配置原样展示地址，同时避免将系统暴露为任意协议跳板。
     * </p>
     */
    private Boolean launchable;

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 登录密码。
     */
    private String password;

    /**
     * 排序值。
     */
    private Integer sort;
}
