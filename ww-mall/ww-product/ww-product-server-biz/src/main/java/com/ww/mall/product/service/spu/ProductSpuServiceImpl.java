package com.ww.mall.product.service.spu;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.IdUtil;
import com.ww.app.mybatis.common.AppPlusPageResult;
import com.ww.mall.product.dao.spu.ProductSpuMapper;
import com.ww.mall.product.entity.brand.ProductBrand;
import com.ww.mall.product.entity.category.ProductCategory;
import com.ww.mall.product.entity.spu.ProductSpu;
import com.ww.mall.product.enums.SpuStatus;
import com.ww.mall.product.service.brand.ProductBrandService;
import com.ww.mall.product.service.category.ProductCategoryService;
import com.ww.mall.product.service.sku.ProductSkuService;
import com.ww.mall.product.view.bo.ProductSkuBO;
import com.ww.mall.product.view.bo.ProductSpuBO;
import com.ww.mall.product.view.bo.ProductSpuStatusBO;
import com.ww.mall.product.view.query.ProductSpuPageQuery;
import com.ww.mall.product.view.vo.ProductSpuPageAdminVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.ww.app.common.utils.CollectionUtils.getMinValue;
import static com.ww.mall.product.entity.category.ProductCategory.CATEGORY_LEVEL;
import static com.ww.mall.product.enums.ErrorCodeConstants.SPU_NOT_EXISTS;
import static com.ww.mall.product.enums.ErrorCodeConstants.SPU_SAVE_FAIL_CATEGORY_LEVEL_ERROR;

/**
 * @author ww
 * @create 2025-09-09 16:05
 * @description:
 */
@Slf4j
@Service
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu> implements ProductSpuService {

    @Resource
    private ProductSpuMapper productSpuMapper;

    @Resource
    private ProductCategoryService productCategoryService;

    @Resource
    private ProductBrandService productBrandService;

    @Resource
    private ProductSkuService productSkuService;

    @Override
    public AppPageResult<ProductSpuPageAdminVO> page(ProductSpuPageQuery productSpuPageQuery) {
        IPage<ProductSpu> page = new Page<>(productSpuPageQuery.getPageNum(), productSpuPageQuery.getPageSize());
        this.page(page, productSpuPageQuery.buildQuery());
        return new AppPlusPageResult<>(page, this::getProductSpuAdminVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(ProductSpuBO productSpuBO) {
        // 商品分类校验
        validateCategory(productSpuBO.getCategoryId());
        // 商品品牌校验
        validateBrand(productSpuBO.getBrandId());
        // 校验sku
        List<ProductSkuBO> skus = productSpuBO.getSkus();
        productSkuService.validateSkuList(skus, productSpuBO.getSpecType());

        ProductSpu spu = BeanUtil.toBean(productSpuBO, ProductSpu.class);
        // 初始化 SPU 中 SKU 相关属性
        initSpuFromSkus(spu, skus);
        // 插入 SPU
        this.save(spu);
        // 插入 SKU
        productSkuService.createSkuList(spu.getId(), skus);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ProductSpuBO productSpuBO) {
        // 校验 SPU 是否存在
        ProductSpu spu = validateSpuExists(productSpuBO.getId());
        // 商品分类校验
        validateCategory(productSpuBO.getCategoryId());
        // 商品品牌校验
        validateBrand(productSpuBO.getBrandId());
        // 校验sku
        List<ProductSkuBO> skus = productSpuBO.getSkus();
        productSkuService.validateSkuList(skus, productSpuBO.getSpecType());

        // 更新 SPU
        ProductSpu updateObj = BeanUtil.toBean(productSpuBO, ProductSpu.class);
        updateObj.setStatus(spu.getStatus());
        initSpuFromSkus(updateObj, skus);
        this.updateById(updateObj);
        // 批量更新 SKU
        productSkuService.updateSkuList(updateObj.getId(), productSpuBO.getSkus());
        return true;
    }

    @Override
    public ProductSpu get(Long id) {
        return this.getById(id);
    }

    @Override
    public void updateSpuStatus(ProductSpuStatusBO productSpuStatusBO) {
        validateSpuExists(productSpuStatusBO.getId());

        ProductSpu productSpuDO = productSpuMapper.selectById(productSpuStatusBO.getId());
        productSpuDO.setStatus(productSpuStatusBO.getStatus());
        productSpuMapper.updateById(productSpuDO);
    }

    @Override
    public Long getSpuCountByCategoryId(Long categoryId) {
        return this.count(new LambdaQueryWrapper<ProductSpu>().eq(ProductSpu::getCategoryId, categoryId));
    }

    @Override
    public Long getSpuCountByBrandId(Long brandId) {
        return this.count(new LambdaQueryWrapper<ProductSpu>().eq(ProductSpu::getBrandId, brandId));
    }

    @Override
    public void updateSalesCount(Long id, int incrCount) {
        productSpuMapper.updateSalesCount(id, incrCount);
    }

    @Override
    public void updateBrowseCount(Long id, int incrCount) {
        productSpuMapper.updateBrowseCount(id, incrCount);
    }

    private void validateCategory(Long categoryId) {
        productCategoryService.validateCategory(categoryId);
        // 校验层级
        if (productCategoryService.getCategoryLevel(categoryId) < CATEGORY_LEVEL) {
            throw new ApiException(SPU_SAVE_FAIL_CATEGORY_LEVEL_ERROR);
        }
    }

    private void validateBrand(Long brandId) {
        productBrandService.validateProductBrand(brandId);
    }

    private ProductSpu validateSpuExists(Long id) {
        ProductSpu spu = this.getById(id);
        if (spu == null) {
            throw new ApiException(SPU_NOT_EXISTS);
        }
        return spu;
    }

    /**
     * 基于 SKU 的信息，初始化 SPU 的信息
     * 主要是计数相关的字段，例如说市场价、最大最小价、库存等等
     *
     * @param spu  商品 SPU
     * @param skus 商品 SKU 数组
     */
    private void initSpuFromSkus(ProductSpu spu, List<ProductSkuBO> skus) {
        // sku 单价最低的商品的价格
        spu.setMinPrice(getMinValue(skus, ProductSkuBO::getPrice));
        // 若是 spu 已有状态则不处理
        if (spu.getStatus() == null) {
            // 默认状态为上架
            spu.setStatus(SpuStatus.UP);
            // 默认商品销量
            spu.setSalesCount(0);
            // 默认商品浏览量
            spu.setBrowseCount(0);
            // spuCode
            spu.setSpuCode(IdUtil.nextIdStr());
        }
    }

    private ProductSpuPageAdminVO getProductSpuAdminVO(ProductSpu result) {
        ProductSpuPageAdminVO spuPageVO = new ProductSpuPageAdminVO();
        BeanUtils.copyProperties(result, spuPageVO);
        ProductBrand brand = productBrandService.getById(result.getBrandId());
        ProductCategory category = productCategoryService.getById(result.getCategoryId());
        spuPageVO.setBrandName(brand.getName());
        spuPageVO.setCategoryName(category.getName());
        return spuPageVO;
    }

}
