package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
* @author ww
* @since 2021-03-10
*/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pms_category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * 分类id
    */
    @TableId(value = "cat_id", type = IdType.AUTO)
    private Long catId;

    /**
    * 分类名称
    */
    private String name;

    /**
    * 父分类id
    */
    private Long parentCid;

    /**
    * 层级
    */
    private Integer catLevel;

    /**
    * 是否显示[0-不显示，1显示]
    */
    private Integer showStatus;

    /**
    * 排序
    */
    private Integer sort;

    /**
    * 图标地址
    */
    private String icon;

    /**
    * 计量单位
    */
    private String productUnit;

    /**
    * 商品数量
    */
    private Integer productCount;

    /**
     * 子类集合
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)     // 当此属性不为Empty的时候在json里显示
    @TableField(exist = false)  // 不存在数据库字段属性
    private List<Category> childrens;


}