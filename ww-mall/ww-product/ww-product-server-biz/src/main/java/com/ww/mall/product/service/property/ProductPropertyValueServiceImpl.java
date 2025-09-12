package com.ww.mall.product.service.property;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.product.dao.property.ProductPropertyValueMapper;
import com.ww.mall.product.entity.property.ProductPropertyValue;
import com.ww.mall.product.service.sku.ProductSkuService;
import com.ww.mall.product.controller.admin.property.req.ProductPropertyValueBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.ww.mall.product.enums.ErrorCodeConstants.PROPERTY_VALUE_EXISTS;
import static com.ww.mall.product.enums.ErrorCodeConstants.PROPERTY_VALUE_NOT_EXISTS;

/**
 * @author ww
 * @create 2025-09-08 15:36
 * @description:
 */
@Slf4j
@Service
public class ProductPropertyValueServiceImpl extends ServiceImpl<ProductPropertyValueMapper, ProductPropertyValue> implements ProductPropertyValueService {

    @Resource
    private ProductSkuService productSkuService;

    @Override
    public boolean add(ProductPropertyValueBO productPropertyValueBO) {
        // 如果已经添加过该属性值，直接返回
        ProductPropertyValue value = this.getOne(new LambdaQueryWrapper<ProductPropertyValue>()
                .eq(ProductPropertyValue::getPropertyId, productPropertyValueBO.getPropertyId())
                .eq(ProductPropertyValue::getName, productPropertyValueBO.getName())
        );
        if (value != null) {
            throw new ApiException(PROPERTY_VALUE_EXISTS);
        }

        ProductPropertyValue productPropertyValue = BeanUtil.toBean(productPropertyValueBO, ProductPropertyValue.class);
        this.save(productPropertyValue);
        return true;
    }

    @Override
    public boolean update(ProductPropertyValueBO productPropertyValueBO) {
        validatePropertyValueExists(productPropertyValueBO.getId());
        // 校验名字唯一
        ProductPropertyValue value = this.getOne(new LambdaQueryWrapper<ProductPropertyValue>()
                .eq(ProductPropertyValue::getPropertyId, productPropertyValueBO.getPropertyId())
                .eq(ProductPropertyValue::getName, productPropertyValueBO.getName())
        );
        if (value != null && !value.getId().equals(productPropertyValueBO.getId())) {
            throw new ApiException(PROPERTY_VALUE_EXISTS);
        }

        ProductPropertyValue updateObj = BeanUtil.toBean(productPropertyValueBO, ProductPropertyValue.class);
        this.save(updateObj);
        // 更新 sku 相关属性
        productSkuService.updateSkuPropertyValue(productPropertyValueBO.getId(), productPropertyValueBO.getName());
        return true;
    }

    private void validatePropertyValueExists(Long id) {
        if (this.getById(id) == null) {
            throw new ApiException(PROPERTY_VALUE_NOT_EXISTS);
        }
    }

}
