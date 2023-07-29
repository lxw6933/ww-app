package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.web.cmmon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@TableName("t_brand")
@EqualsAndHashCode(callSuper = true)
public class Brand extends BaseEntity {

    /**
     * 品牌名
     */
    private String brandName;

    /**
     * 品牌logo地址
     */
    private String logo;

    /**
     * 介绍
     */
    private String desc;

    /**
     * 显示状态[0-不显示；1-显示]
     */
    private Boolean state;

    /**
     * 排序
     */
    private Integer sort;

}