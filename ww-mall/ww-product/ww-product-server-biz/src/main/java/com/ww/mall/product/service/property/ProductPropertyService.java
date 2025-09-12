package com.ww.mall.product.service.property;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.product.entity.property.ProductProperty;
import com.ww.mall.product.controller.admin.property.req.ProductPropertyBO;

/**
 * @author ww
 * @create 2025-09-08 15:34
 * @description:
 */
public interface ProductPropertyService extends IService<ProductProperty> {

    boolean add(ProductPropertyBO productPropertyBO);

    boolean update(ProductPropertyBO productPropertyBO);

}
