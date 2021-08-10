package com.ww.mall.config.drools.entity;

import lombok.Data;

/**
 * @description:
 * @author: ww
 * @create: 2021/5/23 下午2:56
 **/
@Data
public class Calculation {

    /**
     * 税前工资
     */
    private Double wage;
    /**
     * 应纳税所得额
     */
    private Double wagemore;
    /**
     * 税率
     */
    private Double cess;
    /**
     * 速算扣除数
     */
    private Double preminus;
    /**
     * 扣税额
     */
    private Double wageminus;
    /**
     * 税后工资
     */
    private Double actualwage;

}
