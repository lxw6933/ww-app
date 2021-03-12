package com.ww.mall.product.controller;

import com.ww.mall.product.service.BrandService;
import com.ww.mall.product.entity.Brand;
import com.ww.mall.common.constant.R;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
* @author ww
* @since 2021-03-10
*/
@Slf4j
@Api(tags = "品牌")
@RestController
@RequestMapping("brand")
public class BrandController {

    @Autowired
    public BrandService brandService;

    @ApiOperation(value = "新增")
    @PostMapping("/save")
    public R save(@Valid @RequestBody Brand brand){
        brandService.save(brand);
        return R.ok("保存成功");
    }

    @ApiOperation(value = "根据id删除")
    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        brandService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation(value = "条件查询")
    @PostMapping("/get")
    public R list(@RequestBody Brand brand){
        List<Brand> brandList = brandService.list(new QueryWrapper<>(brand));
        return R.ok("查询成功").put("data",brandList);
    }

    @ApiOperation(value = "条件列表（分页）")
    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody Brand brand){
        IPage<Brand> page = brandService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(brand));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @ApiOperation(value = "详情")
    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        Brand brand = brandService.getById(id);
        return R.ok("查询成功").put("data",brand);
    }

    @ApiOperation(value = "根据id修改")
    @PostMapping("/update")
    public R update(@RequestBody Brand brand){
        brandService.updateById(brand);
        return R.ok("更新成功");
    }
}
