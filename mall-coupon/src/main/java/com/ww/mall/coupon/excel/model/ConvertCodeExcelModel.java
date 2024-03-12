package com.ww.mall.coupon.excel.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author ww
 * @create 2024-03-12- 11:52
 * @description:
 */
@Data
public class ConvertCodeExcelModel {

    @ExcelProperty(value = "序号", index = 0)
    private Integer no;

    @ExcelProperty(value = "优惠券券码", index = 1)
    private String convertCode;

}
