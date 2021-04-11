package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.constant.R;
import com.ww.mall.product.entity.Category;
import com.ww.mall.product.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
* @author ww
* @since 2021-03-10
*/
@Slf4j
@Api(tags = "商品三级分类")
@RestController
@RequestMapping("category")
public class CategoryController {

    @Autowired
    public CategoryService categoryService;

    @ApiOperation(value = "新增")
    @PostMapping("/save")
    public R save(@RequestBody Category category){
        categoryService.save(category);
        return R.ok("保存成功");
    }

    @ApiOperation(value = "根据id删除")
    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        categoryService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation(value = "条件查询")
    @PostMapping("/get")
    public R list(@RequestBody Category category){
        List<Category> categoryList = categoryService.list(new QueryWrapper<>(category));
        return R.ok("查询成功").put("data",categoryList);
    }

    @ApiOperation(value = "条件列表（分页）")
    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody Category category){
        IPage<Category> page = categoryService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(category));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @ApiOperation(value = "详情")
    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        Category category = categoryService.getById(id);
        return R.ok("查询成功").put("data",category);
    }

    @ApiOperation(value = "根据id修改")
    @PostMapping("/update")
    public R update(@RequestBody Category category){
        categoryService.updateById(category);
        return R.ok("更新成功");
    }

    @GetMapping("/list/tree")
    public R listWithTree(){
        List<Category> categories = categoryService.listWithTree();
        return R.ok("查询成功").put("data",categories);
    }

}
