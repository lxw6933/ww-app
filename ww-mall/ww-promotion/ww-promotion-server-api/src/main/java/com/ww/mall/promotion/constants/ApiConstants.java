package com.ww.mall.promotion.constants;

import com.ww.app.common.constant.RpcConstants;

/**
 * 拼团 RPC 接口常量。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 统一维护拼团服务名与 RPC 前缀，避免上下游各自硬编码造成路由不一致
 */
public class ApiConstants {

    /**
     * 拼团服务注册名。
     */
    public static final String NAME = "promotion-server";

    /**
     * 拼团 RPC 接口统一前缀。
     */
    public static final String PREFIX = RpcConstants.RPC_API_PREFIX + "/promotion/group";

    private ApiConstants() {
    }
}
