package com.ww.app.open.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户信息实体
 * 
 * @author ww
 * @create 2024-05-25 15:22
 * @description: 
 * 商户信息是开放平台的核心实体，用于管理第三方商户的基本信息和RSA密钥对。
 * 
 * <p><b>业务流程：</b></p>
 * <ol>
 *   <li>商户入驻：调用 saveBusinessClient 方法创建商户信息</li>
 *   <li>自动生成密钥对：如果商户未提供RSA密钥对，系统会自动生成2048位的RSA密钥对</li>
 *   <li>密钥分发：将私钥安全地分发给商户，公钥保存在系统中用于验签</li>
 *   <li>应用注册：商户可以使用 sysCode 注册多个应用（OpenApplication）</li>
 * </ol>
 * 
 * <p><b>密钥用途：</b></p>
 * <ul>
 *   <li>privateKey（私钥）：由商户保管，用于对API请求进行数字签名</li>
 *   <li>publicKey（公钥）：保存在系统中，用于验证商户的签名</li>
 * </ul>
 * 
 * <p><b>与OpenApplication的关系：</b></p>
 * <ul>
 *   <li>一个商户（BusinessClientInfo）可以拥有多个应用（OpenApplication）</li>
 *   <li>所有应用共享商户的RSA密钥对进行签名验证</li>
 *   <li>应用级别的 appSecret 目前仅用于应用身份标识，不参与签名验证</li>
 * </ul>
 */
@Data
@TableName("open_business_client_info")
@EqualsAndHashCode(callSuper = true)
public class BusinessClientInfo extends BaseEntity {

    /**
     * 商户编码（唯一标识，8位）
     * 用于关联应用（OpenApplication.sysCode）
     */
    private String sysCode;

    /**
     * 商户名称
     */
    private String businessName;

    /**
     * RSA公钥（Base64编码）
     * 用于验证商户的签名，保存在系统中
     * 如果商户未提供，系统会自动生成
     */
    private String publicKey;

    /**
     * RSA私钥（Base64编码）
     * 由商户保管，用于对API请求进行数字签名
     * 如果商户未提供，系统会自动生成
     * 注意：私钥需要安全地分发给商户，系统不应长期保存私钥
     */
    private String privateKey;

    /**
     * 状态（true-启用，false-禁用）
     */
    private Boolean status;

}
