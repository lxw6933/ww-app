package com.ww.mall.product.service.brand;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.app.common.common.AppPageResult;
import com.ww.mall.product.entity.brand.ProductBrand;
import com.ww.mall.product.view.bo.ProductBrandBO;
import com.ww.mall.product.view.query.ProductBrandPageQuery;
import com.ww.mall.product.view.vo.ProductBrandVO;

/**
 * @author ww
 * @create 2025-09-05 23:57
 * @description:
 */
public interface ProductBrandService extends IService<ProductBrand> {

    AppPageResult<ProductBrandVO> page(ProductBrandPageQuery productBrandPageQuery);

    boolean add(ProductBrandBO productBrandBO);

    boolean update(ProductBrandBO productBrandBO);

}
