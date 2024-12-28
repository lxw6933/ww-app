package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.product.enums.AttrType;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@TableName("t_attr")
public class Attr implements Serializable {

    /**
     * 属性id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 属性名
     */
    private String attrName;

    /**
     * 属性图标
     */
    private String icon;

    /**
     * 可选值列表[用逗号分隔]
     */
    private String valueSelect;

    /**
     * 属性类型
     */
    private AttrType attrType;

    /**
     * 启用状态[0 - 禁用，1 - 启用]
     */
    private Boolean state;

    /**
     * 所属分类
     */
    private Long categoryId;

}