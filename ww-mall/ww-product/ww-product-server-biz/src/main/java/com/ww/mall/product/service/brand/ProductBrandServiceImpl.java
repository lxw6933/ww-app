package com.ww.mall.product.service.brand;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.exception.ApiException;
import com.ww.app.mybatis.common.AppPlusPageResult;
import com.ww.mall.product.dao.ProductBrandMapper;
import com.ww.mall.product.entity.brand.ProductBrand;
import com.ww.mall.product.view.bo.ProductBrandBO;
import com.ww.mall.product.view.query.ProductBrandPageQuery;
import com.ww.mall.product.view.vo.ProductBrandVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import static com.ww.mall.product.enums.ErrorCodeConstants.BRAND_NAME_EXISTS;
import static com.ww.mall.product.enums.ErrorCodeConstants.BRAND_NOT_EXISTS;

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
        this.page(page);
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

        ProductBrand brand = BeanUtil.toBean(productBrandBO, ProductBrand.class);
        this.save(brand);
        return true;
    }

    @Override
    public boolean update(ProductBrandBO productBrandBO) {
        // 校验存在
        validateBrandExists(productBrandBO.getId());
        validateBrandNameUnique(productBrandBO.getId(), productBrandBO.getName());

        ProductBrand updateObj = BeanUtil.toBean(productBrandBO, ProductBrand.class);
        this.updateById(updateObj);
        return true;
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
