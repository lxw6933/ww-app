package com.ww.mall.product.view.vo;

import com.ww.mall.product.enums.SpuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author ww
 * @create 2025-09-10 10:28
 * @description:
 */
@Data
public class ProductSpuPageAdminVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "商品编码")
    private String spuCode;

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "商品类型【虚拟、实物】", requiredMode = Schema.RequiredMode.REQUIRED)
    private SpuType spuType;

    @Schema(description = "商品分类", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;

    @Schema(description = "商品品牌", requiredMode = Schema.RequiredMode.REQUIRED)
    private String brandName;

    @Schema(description = "商品封面图", requiredMode = Schema.RequiredMode.REQUIRED)
    private String img;

    @Schema(description = "商品简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String introduction;

    @Schema(description = "规格类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean specType;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date updateTime;

}
