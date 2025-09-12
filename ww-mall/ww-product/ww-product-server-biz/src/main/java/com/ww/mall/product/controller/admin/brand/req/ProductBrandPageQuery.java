package com.ww.mall.product.controller.admin.brand.req;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ww.app.common.common.AppPage;
import com.ww.app.mybatis.core.LambdaQueryWrapperX;
import com.ww.mall.product.entity.brand.ProductBrand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2025-09-06 16:27
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductBrandPageQuery extends AppPage {

    @Schema(description = "品牌名称")
    private String name;

    public Wrapper<ProductBrand> buildQuery() {
        return new LambdaQueryWrapperX<ProductBrand>()
                .likeIfPresent(ProductBrand::getName, this.name);
    }

}
