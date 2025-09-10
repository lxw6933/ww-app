package com.ww.mall.product.service.category;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.product.entity.category.ProductCategory;
import com.ww.mall.product.view.bo.ProductCategoryBO;
import com.ww.mall.product.view.query.ProductCategoryQuery;
import com.ww.mall.product.view.vo.ProductCategoryVO;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /**
     * category tree
     *
     * @return List
     */
    List<ProductCategoryVO> listCategoryTree();

    List<ProductCategoryVO> listCategoryTree(ProductCategoryQuery productCategoryQuery);

    /**
     * 校验商品分类
     *
     * @param id 分类编号
     */
    void validateCategory(Long id);

    /**
     * 获得商品分类的层级
     *
     * @param id 编号
     * @return 商品分类的层级
     */
    Integer getCategoryLevel(Long id);

    /**
     * 新增
     *
     * @param productCategoryBO productCategoryBO
     * @return boolean
     */
    boolean add(ProductCategoryBO productCategoryBO);

    /**
     * 编辑
     *
     * @param productCategoryBO productCategoryBO
     * @return boolean
     */
    boolean update(ProductCategoryBO productCategoryBO);
}
