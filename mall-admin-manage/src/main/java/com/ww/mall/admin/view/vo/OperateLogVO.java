package com.ww.mall.admin.view.vo;

import lombok.Data;

/**
 * @author ww
 * @create 2024-09-18- 18:11
 * @description: 操作记录
 */
@Data
public class OperateLogVO {

    /**
     * traceId
     */
    private String traceId;

    /**
     * 操作用户id
     */
    private Long userId;

    /**
     * 操作用户名称
     */
    private String nickName;

    /**
     * 操作模块类型
     */
    private String type;

    /**
     * 操作名
     */
    private String subType;

    /**
     * 操作模块业务编号
     */
    private Long bizId;

    /**
     * 日志内容，记录整个操作的明细
     */
    private String action;

    /**
     * 拓展字段，有些复杂的业务，需要记录一些字段 ( JSON 格式 )
     * <p>
     * 例如说，记录订单编号，{ orderId: "1"}
     */
    private String extra;

    /**
     * 请求方法名
     */
    private String requestMethod;

    /**
     * 请求地址
     */
    private String requestUrl;

    /**
     * 用户 IP
     */
    private String userIp;

    /**
     * 浏览器 UA
     */
    private String userAgent;

    /**
     * 日志时间
     */
    private String createTime;

}
