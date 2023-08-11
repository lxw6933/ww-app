package com.ww.mall.order.view.bo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-08-11- 14:15
 * @description:
 */
@Data
public class OrderInvoiceBO {

    /**
     * 发票类型
     */
    @NotNull(message = "发票类型不能为空")
    private String invoiceType;

    /**
     * 发票抬头类型
     */
    @NotNull(message = "发票抬头不能为空")
    private String invoiceHeadType;

    /**
     * 抬头名称（个人和企业）
     */
    @NotNull(message = "抬头不能为空")
    private String headName;

    /**
     * 税号（企业）
     */
    private String taxNo;

    /**
     * 手机号（个人和企业）
     */
    private String phone;

    /**
     * 邮箱地址（电子个人、电子企业选填）
     */
    private String email;

    // ============================电子企业选填===========================
    /**
     * 银行名称
     */
    private String bankName;

    /**
     * 银行卡号
     */
    private String bankNo;

    /**
     * 企业电话
     */
    private String companyPhone;

    /**
     * 企业地址
     */
    private String companyAddress;
    // ============================电子企业选填===========================

    // ============================纸质个人===========================
    /**
     * 收票人
     */
    private String receiver;

    /**
     * 省份
     */
    private String provinceName;

    /**
     * 城市
     */
    private String cityName;

    /**
     * 地区
     */
    private String areaName;

    /**
     * 街区
     */
    private String streetName;

    /**
     * 详细地址
     */
    private String detailAddress;
    // ============================纸质个人===========================

}
