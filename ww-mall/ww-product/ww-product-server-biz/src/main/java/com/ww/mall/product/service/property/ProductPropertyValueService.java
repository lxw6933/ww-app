package com.ww.mall.product.service.property;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.product.entity.property.ProductPropertyValue;
import com.ww.mall.product.controller.admin.property.req.ProductPropertyValueBO;

/**
 * @author ww
 * @create 2025-09-08 15:34
 * @description:
 */
public interface ProductPropertyValueService extends IService<ProductPropertyValue> {

    boolean add(ProductPropertyValueBO productPropertyValueBO);

    boolean update(ProductPropertyValueBO productPropertyValueBO);

}
