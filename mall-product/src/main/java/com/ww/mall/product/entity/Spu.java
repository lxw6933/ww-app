package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.product.enums.SpuStatus;
import com.ww.mall.product.enums.SpuType;
import com.ww.mall.web.cmmon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@TableName("t_spu")
@EqualsAndHashCode(callSuper = true)
public class Spu extends BaseEntity {

    /**
     * 商家id
     */
    private Long merchantId;

    /**
     * 商品名称
     */
    private String spuName;

    /**
     * 所属分类id
     */
    private Long catalogId;

    /**
     * 品牌id
     */
    private Long brandId;

    /**
     * 商品编码
     */
    private String spuCode;

    /**
     * 商品类型【虚拟、实物】
     */
    private SpuType spuType;

    /**
     * 商品状态【上下架、冻结】
     */
    private SpuStatus spuStatus;

    /**
     * 商品单位【默认：1 件】
     */
    private Long unit;

    /**
     * 运费模板【默认：1 包邮】
     */
    private Long fareTmpId;

    /**
     * 是否有效
     */
    private Boolean valid;

}