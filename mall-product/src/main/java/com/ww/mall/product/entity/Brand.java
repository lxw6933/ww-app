package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
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
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pms_brand")
public class Brand implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * 品牌id
    */
    @TableId(value = "brand_id", type = IdType.AUTO)
    private Long brandId;

    /**
    * 品牌名
    */
    @Email
    @NotBlank(message = "品牌名不能为空")
    private String name;

    /**
    * 品牌logo地址
    */
    private String logo;

    /**
    * 介绍
    */
    private String descript;

    /**
    * 显示状态[0-不显示；1-显示]
    */
    private Integer showStatus;

    /**
    * 检索首字母
    */
    private String firstLetter;

    /**
    * 排序
    */
    private Integer sort;


}