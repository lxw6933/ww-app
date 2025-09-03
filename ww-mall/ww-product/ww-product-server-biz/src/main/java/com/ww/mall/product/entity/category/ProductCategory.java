package com.ww.mall.product.entity.category;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ww.app.mybatis.common.BaseEntity;
import com.ww.mall.product.entity.test.Category;
import com.ww.mall.product.enums.CategoryLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-03- 09:56
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("product_category")
public class ProductCategory extends BaseEntity {

    /**
     * 父分类编号 - 根分类
     */
    public static final Long PARENT_ID_NULL = 0L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类id
     */
    private Long parentId;

    /**
     * 层级
     */
    private CategoryLevel level;

    /**
     * 是否显示[0-不显示，1显示]
     */
    private Boolean status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 图标地址
     */
    private String icon;

    /**
     * 子类集合
     */
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Category> childrens;

}
