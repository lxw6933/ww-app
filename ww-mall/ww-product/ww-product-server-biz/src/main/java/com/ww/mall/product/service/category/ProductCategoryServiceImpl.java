package com.ww.mall.product.service.category;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.product.controller.admin.category.req.ProductCategoryBO;
import com.ww.mall.product.controller.admin.category.req.ProductCategoryQuery;
import com.ww.mall.product.controller.admin.category.res.ProductCategoryVO;
import com.ww.mall.product.convert.category.ProductCategoryConvert;
import com.ww.mall.product.dao.category.ProductCategoryMapper;
import com.ww.mall.product.entity.category.ProductCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.filterList;
import static com.ww.mall.product.entity.category.ProductCategory.PARENT_ID_NULL;
import static com.ww.mall.product.enums.ErrorCodeConstants.*;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Slf4j
@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {

    @Resource
    private ProductCategoryService productCategoryService;

    @Override
    @Cacheable(value = "categoryTree", key="'all'", unless = "#result==null or #result?.size() == 0")
    public List<ProductCategoryVO> listCategoryTree() {
        // 获取所有Category
//        List<ProductCategoryVO> allCategory = convertList(this.list(), res -> BeanUtil.toBean(res, ProductCategoryVO.class));
        List<ProductCategoryVO> allCategory = ProductCategoryConvert.INSTANCE.convertList(this.list());
        // 遍历所有一级类目，并设置子类集合【递归设置】
        return allCategory.stream()
                .filter(res -> PARENT_ID_NULL.equals(res.getParentId()))
                .peek(res -> {
                    List<ProductCategoryVO> children = getChildrenList(res, allCategory);
                    res.setChildren(children);
                })
                .sorted(Comparator.comparingInt(res -> (res.getSort() == null ? 0 : res.getSort())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductCategoryVO> listCategoryTree(ProductCategoryQuery productCategoryQuery) {
        String searchCategoryName = productCategoryQuery.getName();
        List<ProductCategoryVO> categoryList = productCategoryService.listCategoryTree();
        if (StrUtil.isEmpty(searchCategoryName)) {
            return categoryList;
        }
        String highLightStr = "<span class='category-highlight'>" + searchCategoryName + "</span>";
        return filterList(categoryList, res -> {
            boolean one = res.getName().contains(searchCategoryName);
            if (one) {
                res.getName().replace(searchCategoryName, highLightStr);
            }
            boolean flag = false;
            for (ProductCategoryVO twoChildren : res.getChildren()) {
                boolean two = twoChildren.getName().contains(searchCategoryName);
                if (two) {
                    flag = true;
                    twoChildren.getName().replace(searchCategoryName, highLightStr);
                }
                for (ProductCategoryVO threeChildren : twoChildren.getChildren()) {
                    boolean three = threeChildren.getName().contains(searchCategoryName);
                    if (three) {
                        flag = true;
                        threeChildren.getName().replace(searchCategoryName, highLightStr);
                    }
                }
            }
            return one || flag;
        });
    }

    @Override
    public void validateCategory(Long id) {
        ProductCategory category = this.getById(id);
        if (category == null) {
            throw new ApiException(CATEGORY_NOT_EXISTS);
        }
        if (Objects.equals(category.getStatus(), Boolean.FALSE)) {
            throw new ApiException(CATEGORY_DISABLED);
        }
    }

    @Override
    public Integer getCategoryLevel(Long id) {
        if (Objects.equals(id, PARENT_ID_NULL)) {
            return 0;
        }
        int level = 1;
        // for 的原因，是因为避免脏数据，导致可能的死循环。
        for (int i = 0; i < Byte.MAX_VALUE; i++) {
            // 如果没有父节点，break 结束
            ProductCategory category = this.getById(id);
            if (category == null || Objects.equals(category.getParentId(), PARENT_ID_NULL)) {
                break;
            }
            // 继续递归父节点
            level++;
            id = category.getParentId();
        }
        return level;
    }

    @Override
    @CacheEvict(value = "categoryTree", key="'all'")
    public boolean add(ProductCategoryBO productCategoryBO) {
        // 校验父分类存在
        validateParentProductCategory(productCategoryBO.getParentId());

        ProductCategory category = ProductCategoryConvert.INSTANCE.convert(productCategoryBO);
        this.save(category);
        return true;
    }

    @Override
    @CacheEvict(value = "categoryTree", key="'all'")
    public boolean update(ProductCategoryBO productCategoryBO) {
        // 校验分类是否存在
        validateProductCategoryExists(productCategoryBO.getId());
        // 校验父分类存在
        validateParentProductCategory(productCategoryBO.getParentId());

        ProductCategory updateObj = ProductCategoryConvert.INSTANCE.convert(productCategoryBO);
        this.updateById(updateObj);
        return true;
    }

    /**
     * 获取当前类目下所有子类目集合
     *
     * @param curCategory 当前类目【需要set子类目的父类目】
     * @param allCategory 所有类目
     * @return 当前类目下所有子类目集合
     */
    private List<ProductCategoryVO> getChildrenList(ProductCategoryVO curCategory, List<ProductCategoryVO> allCategory) {
        return allCategory.stream()
                .filter(res -> res.getParentId().equals(curCategory.getId()))
                .peek(res -> res.setChildren(getChildrenList(res, allCategory)))
                .sorted(Comparator.comparingInt(res -> (res.getSort() == null ? 0 : res.getSort())))
                .collect(Collectors.toList());
    }

    private void validateParentProductCategory(Long parentId) {
        if (Objects.equals(parentId, PARENT_ID_NULL)) {
            return;
        }
        // 父分类不存在
        ProductCategory category = this.getById(parentId);
        if (category == null) {
            throw new ApiException(CATEGORY_PARENT_NOT_EXISTS);
        }
        // 父分类不能是二级分类
        if (!Objects.equals(category.getParentId(), PARENT_ID_NULL)) {
            throw new ApiException(CATEGORY_PARENT_NOT_FIRST_LEVEL);
        }
    }

    private void validateProductCategoryExists(Long id) {
        ProductCategory category = this.getById(id);
        if (category == null) {
            throw new ApiException(CATEGORY_NOT_EXISTS);
        }
    }

}
