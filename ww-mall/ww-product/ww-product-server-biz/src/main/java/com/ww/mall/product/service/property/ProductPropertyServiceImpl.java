package com.ww.mall.product.service.property;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.product.dao.property.ProductPropertyMapper;
import com.ww.mall.product.entity.property.ProductProperty;
import com.ww.mall.product.service.sku.ProductSkuService;
import com.ww.mall.product.view.bo.ProductPropertyBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static com.ww.mall.product.enums.ErrorCodeConstants.PROPERTY_EXISTS;
import static com.ww.mall.product.enums.ErrorCodeConstants.PROPERTY_NOT_EXISTS;

/**
 * @author ww
 * @create 2025-09-08 15:36
 * @description:
 */
@Slf4j
@Service
public class ProductPropertyServiceImpl extends ServiceImpl<ProductPropertyMapper, ProductProperty> implements ProductPropertyService {

    @Resource
    private ProductSkuService productSkuService;

    @Override
    public boolean add(ProductPropertyBO productPropertyBO) {
        ProductProperty productProperty = this.getOne(new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getName, productPropertyBO.getName()));
        if (productProperty != null) {
            throw new ApiException(PROPERTY_EXISTS);
        }

        ProductProperty property = BeanUtil.toBean(productPropertyBO, ProductProperty.class);
        this.save(property);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ProductPropertyBO productPropertyBO) {
        validatePropertyExists(productPropertyBO.getId());

        // 校验名字重复
        ProductProperty productProperty = this.getOne(new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getName, productPropertyBO.getName()));
        if (productProperty != null && ObjUtil.notEqual(productProperty.getId(), productPropertyBO.getId())) {
            throw new ApiException(PROPERTY_EXISTS);
        }

        ProductProperty updateObj = BeanUtil.toBean(productPropertyBO, ProductProperty.class);
        this.updateById(updateObj);
        // 更新 sku 相关属性
        productSkuService.updateSkuProperty(productPropertyBO.getId(), productPropertyBO.getName());
        return true;
    }

    private void validatePropertyExists(Long id) {
        if (this.getById(id) == null) {
            throw new ApiException(PROPERTY_NOT_EXISTS);
        }
    }

}
