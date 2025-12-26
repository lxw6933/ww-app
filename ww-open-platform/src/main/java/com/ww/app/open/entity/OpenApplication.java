package com.ww.app.open.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放平台应用实体
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 应用管理实体，用于管理第三方应用的注册信息
 */
@Data
@TableName("open_application")
@EqualsAndHashCode(callSuper = true)
public class OpenApplication extends BaseEntity {

    /**
     * 应用编码（唯一标识）
     */
    private String appCode;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 商户编码（关联）
     * @see BusinessClientInfo
     */
    private String sysCode;

    /**
     * 应用描述
     */
    private String description;

    /**
     * 应用密钥
     * 注意：当前签名验证使用的是商户级别的RSA密钥对（BusinessClientInfo中的publicKey/privateKey）
     * 此字段目前用于应用身份标识，未来可用于其他验证场景（如HMAC签名、Token生成等）
     */
    private String appSecret;

    /**
     * 应用状态：0-待审核，1-已启用，2-已禁用，3-已拒绝
     */
    private Integer status;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * IP白名单（多个IP用逗号分隔）
     */
    private String ipWhitelist;

    /**
     * 每日调用限额
     */
    private Long dailyLimit;

    /**
     * 每分钟调用限额
     */
    private Long minuteLimit;

    /**
     * 应用版本
     */
    private String appVersion;

    /**
     * 审核意见
     */
    private String auditRemark;

    /**
     * 审核人
     */
    private String auditor;

    /**
     * 审核时间
     */
    private Long auditTime;

    public static LambdaQueryWrapper<OpenApplication> buildAppQueryWrapper(String appCode) {
        LambdaQueryWrapper<OpenApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenApplication::getAppCode, appCode);
        return wrapper;
    }

}

