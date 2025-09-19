package com.ww.mall.product.service.brand;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.exception.ApiException;
import com.ww.app.mybatis.common.AppPlusPageResult;
import com.ww.app.redis.annotation.RedisPublishMsg;
import com.ww.mall.product.constants.RedisChannelConstant;
import com.ww.mall.product.controller.admin.brand.req.ProductBrandBO;
import com.ww.mall.product.controller.admin.brand.req.ProductBrandPageQuery;
import com.ww.mall.product.controller.admin.brand.res.ProductBrandVO;
import com.ww.mall.product.convert.brand.ProductBrandConvert;
import com.ww.mall.product.dao.brand.ProductBrandMapper;
import com.ww.mall.product.entity.brand.ProductBrand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import static com.ww.mall.product.enums.ErrorCodeConstants.*;

/**
 * @author ww
 * @create 2025-09-05 23:57
 * @description:
 */
@Slf4j
@Service
public class ProductBrandServiceImpl extends ServiceImpl<ProductBrandMapper, ProductBrand> implements ProductBrandService {


    @Override
    public AppPageResult<ProductBrandVO> page(ProductBrandPageQuery productBrandPageQuery) {
        IPage<ProductBrand> page = new Page<>(productBrandPageQuery.getPageNum(), productBrandPageQuery.getPageSize());
        this.page(page, productBrandPageQuery.buildQuery());
        return new AppPlusPageResult<>(page, result -> {
            ProductBrandVO brandVO = new ProductBrandVO();
            BeanUtils.copyProperties(result, brandVO);
            return brandVO;
        });
    }

    @Override
    public boolean add(ProductBrandBO productBrandBO) {
        // 校验
        validateBrandNameUnique(null, productBrandBO.getName());

        ProductBrand brand = ProductBrandConvert.INSTANCE.convert(productBrandBO);
        this.save(brand);
        return true;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.BRAND_CACHE_CHANNEL, message = "#productBrandBO.id")
    public boolean update(ProductBrandBO productBrandBO) {
        // 校验存在
        validateBrandExists(productBrandBO.getId());
        validateBrandNameUnique(productBrandBO.getId(), productBrandBO.getName());

        ProductBrand updateObj = ProductBrandConvert.INSTANCE.convert(productBrandBO);
        this.updateById(updateObj);
        return true;
    }

    @Override
    public void validateProductBrand(Long id) {
        ProductBrand brand = this.getById(id);
        if (brand == null) {
            throw new ApiException(BRAND_NOT_EXISTS);
        }
        if (brand.getStatus().equals(Boolean.FALSE)) {
            throw new ApiException(BRAND_DISABLED);
        }
    }

    private void validateBrandExists(Long id) {
        if (this.getById(id) == null) {
            throw new ApiException(BRAND_NOT_EXISTS);
        }
    }

    private void validateBrandNameUnique(Long id, String name) {
        ProductBrand brand = this.getOne(new LambdaQueryWrapper<ProductBrand>().eq(ProductBrand::getName, name));
        if (brand == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的字典类型
        if (id == null) {
            throw new ApiException(BRAND_NAME_EXISTS);
        }
        if (!brand.getId().equals(id)) {
            throw new ApiException(BRAND_NAME_EXISTS);
        }
    }

}
