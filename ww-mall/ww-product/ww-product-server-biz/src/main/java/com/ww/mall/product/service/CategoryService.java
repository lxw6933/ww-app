package com.ww.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.product.entity.test.Category;
import com.ww.mall.product.view.bo.CategoryBO;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
public interface CategoryService extends IService<Category> {

    /**
     * category tree
     *
     * @return List
     */
    List<Category> listCategoryTree();

    List<Category> listCategoryTree(CategoryBO categoryBO);

    /**
     * 新增
     *
     * @param category category
     * @return boolean
     */
    boolean add(Category category);

    /**
     * 编辑
     *
     * @param id id
     * @param category category
     * @return boolean
     */
    boolean modify(Long id, Category category);
}
