package com.ww.mall.product.entity.test;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@TableName("t_spu_desc")
public class SpuInfoDesc implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品id
     */
    private Long spuId;

    /**
     * 商品html介绍
     */
    private String descInfo;

}
