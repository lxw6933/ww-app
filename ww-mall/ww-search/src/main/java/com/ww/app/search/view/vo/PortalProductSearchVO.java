package com.ww.app.search.view.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2024-07-23- 19:13
 * @description:
 */
@Data
public class PortalProductSearchVO {

    private Long spuId;

    private Long smsId;

    private Long skuId;

    private String spuImgUrl;

    private String title;

    private String subTitle;

    private BigDecimal salePrice;

    private BigDecimal suggestSalesPrice;

    private BigDecimal fixMinSalePrice;

    private Integer fixMinIntegral;

    private Boolean sellOutStatus;

}
