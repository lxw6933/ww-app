package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.SpuImages;
import com.ww.mall.product.service.SpuImagesService;
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
@RequestMapping("spu-images")
public class SpuImagesController {

    @Autowired
    public SpuImagesService spuImagesService;

    @PostMapping("/save")
    public R save(@RequestBody SpuImages spuImages){
        spuImagesService.save(spuImages);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        spuImagesService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody SpuImages spuImages){
        List<SpuImages> spuImagesList = spuImagesService.list(new QueryWrapper<>(spuImages));
        return R.ok("查询成功").put("data",spuImagesList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody SpuImages spuImages){
        IPage<SpuImages> page = spuImagesService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(spuImages));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        SpuImages spuImages = spuImagesService.getById(id);
        return R.ok("查询成功").put("data",spuImages);
    }

    @PostMapping("/update")
    public R update(@RequestBody SpuImages spuImages){
        spuImagesService.updateById(spuImages);
        return R.ok("更新成功");
    }
}
