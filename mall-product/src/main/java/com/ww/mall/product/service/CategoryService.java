package com.ww.mall.product.service;

import com.ww.mall.product.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

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

}
