package com.ww.mall.product.service.impl;

import com.ww.mall.product.entity.Category;
import com.ww.mall.product.dao.CategoryMapper;
import com.ww.mall.product.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author ww
* @since 2021-03-10
*/
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    CategoryMapper categoryMapper;

    @Override
    public List<Category> listWithTree() {
        // 获取所有Category
        List<Category> allCategories = categoryMapper.selectList(null);
        //获取所有父类Category，并设置子类集合
        List<Category> allParentCategories = allCategories.stream().filter(res -> {
            return res.getParentCid() == 0;
        }).map(res -> {
            res.setChildrens(getChildrens(res, allCategories));
            return res;
        }).sorted((res1,res2) -> {
            return (res1.getSort() == null ? 0 : res1.getSort()) - (res2.getSort() == null ? 0 : res2.getSort());
        }).collect(Collectors.toList());
        return allParentCategories;
    }

    /**
     * 获取当前category的子类集合
     * @param curCategory
     * @param all
     * @return
     */
    private List<Category> getChildrens(Category curCategory, List<Category> all){
        List<Category> children = all.stream().filter(res -> {
            return res.getParentCid().equals(curCategory.getCatId());
        }).map(res -> {
            res.setChildrens(getChildrens(res, all));
            return res;
        }).sorted((res1, res2) -> {
            return (res1.getSort() == null ? 0 : res1.getSort()) - (res2.getSort() == null ? 0 : res2.getSort());
        }).collect(Collectors.toList());
        return children;
    }


}
