package com.ww.app.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.product.dao.CategoryMapper;
import com.ww.app.product.entity.Category;
import com.ww.app.product.service.CategoryService;
import com.ww.app.product.view.bo.CategoryBO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.filterList;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    @Cacheable(value = "categoryTree", key="'all'", unless = "#result==null or #result?.size() == 0")
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

    @Override
    public List<Category> listCategoryTree(CategoryBO categoryBO) {
        String searchCategoryName = categoryBO.getCategoryName();
        List<Category> categoryList = this.listCategoryTree();
        String highLightStr = "<span class='category-hilight'>" + searchCategoryName + "</span>";
        return filterList(categoryList, res -> {
            boolean one = res.getCategoryName().contains(searchCategoryName);
            if (one) {
                res.getCategoryName().replace(searchCategoryName, highLightStr);
            }
            boolean flag = false;
            for (Category twoChildren : res.getChildrens()) {
                boolean two = twoChildren.getCategoryName().contains(searchCategoryName);
                if (two) {
                    flag = true;
                    twoChildren.getCategoryName().replace(searchCategoryName, highLightStr);
                }
                for (Category threeChildren : twoChildren.getChildrens()) {
                    boolean three = threeChildren.getCategoryName().contains(searchCategoryName);
                    if (three) {
                        flag = true;
                        threeChildren.getCategoryName().replace(searchCategoryName, highLightStr);
                    }
                }
            }
            return one || flag;
        });
    }

    @Override
    @CacheEvict(value = "categoryTree", key="'all'")
    public boolean add(Category category) {
        return true;
    }

    @Override
    @CacheEvict(value = "categoryTree", key="'all'")
    public boolean modify(Long id, Category category) {
        return true;
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
