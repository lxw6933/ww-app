package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.SkuImages;
import com.ww.mall.product.service.SkuImagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
* @author ww
* @since 2021-03-10
*/
@Slf4j
@RestController
@RequestMapping("sku-images")
public class SkuImagesController {

    @Autowired
    public SkuImagesService skuImagesService;

    @PostMapping("/save")
    public R save(@RequestBody SkuImages skuImages){
        skuImagesService.save(skuImages);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        skuImagesService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody SkuImages skuImages){
        List<SkuImages> skuImagesList = skuImagesService.list(new QueryWrapper<>(skuImages));
        return R.ok("查询成功").put("data",skuImagesList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody SkuImages skuImages){
        IPage<SkuImages> page = skuImagesService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(skuImages));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        SkuImages skuImages = skuImagesService.getById(id);
        return R.ok("查询成功").put("data",skuImages);
    }

    @PostMapping("/update")
    public R update(@RequestBody SkuImages skuImages){
        skuImagesService.updateById(skuImages);
        return R.ok("更新成功");
    }
}
