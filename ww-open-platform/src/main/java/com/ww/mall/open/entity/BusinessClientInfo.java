package com.ww.mall.open.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2024-05-25 15:22
 * @description:
 */
@Data
@TableName("open_business_client_info")
@EqualsAndHashCode(callSuper = true)
public class BusinessClientInfo extends BaseEntity {

    /**
     * 商户编码
     */
    private String sysCode;

    /**
     * 商户名称
     */
    private String businessName;

    /**
     * 公钥
     */
    private String publicKey;

    /**
     * 私钥
     */
    private String privateKey;

    /**
     * 状态
     */
    private Boolean status;

}
