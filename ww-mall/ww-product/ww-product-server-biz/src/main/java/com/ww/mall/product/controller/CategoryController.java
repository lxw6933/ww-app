package com.ww.mall.product.controller;

import com.ww.mall.product.entity.test.Category;
import com.ww.mall.product.service.CategoryService;
import com.ww.mall.product.view.bo.CategoryBO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-29- 11:21
 * @description:
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @GetMapping
    public List<Category> list() {
        return categoryService.listCategoryTree();
    }

    @GetMapping("/2")
    public List<Category> list2(CategoryBO categoryBO) {
        return categoryService.listCategoryTree(categoryBO);
    }

    @PutMapping
    public boolean add(@RequestBody Category category) {
        return categoryService.add(category);
    }

    @PutMapping("/{id}")
    public boolean modify(@PathVariable("id") Long id, @RequestBody Category category) {
        return categoryService.modify(id, category);
    }

}
