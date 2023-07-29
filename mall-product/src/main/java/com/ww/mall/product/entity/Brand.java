package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.web.cmmon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @since 2021-03-10
 */
@Data
@TableName("t_brand")
@EqualsAndHashCode(callSuper = true)
public class Brand extends BaseEntity {

    /**
     * 品牌名
     */
    private String name;

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
    private Boolean status;

    /**
     * 排序
     */
    private Integer sort;


}