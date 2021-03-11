package com.ww.mall.product.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("pms_spu_info")
public class SpuInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * 商品id
    */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
    * 商品名称
    */
    private String spuName;

    /**
    * 商品描述
    */
    private String spuDescription;

    /**
    * 所属分类id
    */
    private Long catalogId;

    /**
    * 品牌id
    */
    private Long brandId;

    private BigDecimal weight;

    /**
    * 上架状态[0 - 下架，1 - 上架]
    */
    private Integer publishStatus;

    private Date createTime;

    private Date updateTime;


}