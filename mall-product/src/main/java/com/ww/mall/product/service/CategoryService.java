package com.ww.mall.product.service;

import com.ww.mall.product.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author ww
* @since 2021-03-10
*/
public interface CategoryService extends IService<Category> {

    List<Category> listWithTree();

}
