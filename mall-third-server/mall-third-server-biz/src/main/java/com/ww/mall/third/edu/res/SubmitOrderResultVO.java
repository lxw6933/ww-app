package com.ww.mall.third.edu.res;

import lombok.Data;

/**
 * @author ww
 * @create 2023-03-28- 16:59
 * @description:
 */
@Data
public class SubmitOrderResultVO {

    /**
     * 订单编号
     */
    private String productOrderNo;

    /**
     * 示例：success
     */
    private String status;

}
