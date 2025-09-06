package com.ww.mall.product.view.query;

import com.ww.app.common.common.AppPage;
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

}
