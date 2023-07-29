package com.ww.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.product.dao.CategoryMapper;
import com.ww.mall.product.entity.Category;
import com.ww.mall.product.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ww
 * @since 2021-03-10
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> listCategoryTree() {
        // 获取所有Category
        List<Category> allCategory = this.list();
        // 遍历所有一级类目，并设置子类集合【递归设置】
        return allCategory.stream()
                .filter(res -> res.getPid() == 0)
                .peek(res -> res.setChildrens(getChildrenList(res, allCategory)))
                .sorted(Comparator.comparingInt(res -> (res.getSort() == null ? 0 : res.getSort())))
                .collect(Collectors.toList());
    }

    /**
     * 获取当前类目下所有子类目集合
     *
     * @param curCategory 当前类目【需要set子类目的父类目】
     * @param allCategory 所有类目
     * @return 当前类目下所有子类目集合
     */
    private List<Category> getChildrenList(Category curCategory, List<Category> allCategory) {
        return allCategory.stream()
                .filter(res -> res.getPid().equals(curCategory.getId()))
                .peek(res -> res.setChildrens(getChildrenList(res, allCategory)))
                .sorted(Comparator.comparingInt(res -> (res.getSort() == null ? 0 : res.getSort())))
                .collect(Collectors.toList());
    }

}
