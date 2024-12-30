package com.ww.app.third.edu.req;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-03-29- 11:52
 * @description:
 */
@Data
public class UploadInvoiceReqBO {

    @JSONField(ordinal = 3)
    private String cmd;

    @JSONField(ordinal = 4)
    @NotNull(message = "发票链接地址不能为空")
    private String invoiceLink;

    @JSONField(ordinal = 1)
    @NotNull(message = "订单编码不能为空")
    private String productOrderNo;

    @JSONField(ordinal = 6)
    @NotNull(message = "发票编号不能为空")
    private String invoiceCode;

    @JSONField(ordinal = 5)
    @NotNull(message = "发票日期不能为空")
    private String invoiceDate;

    @JSONField(ordinal = 2)
    @NotNull(message = "发票类型不能为空")
    private String invoiceType;

}
