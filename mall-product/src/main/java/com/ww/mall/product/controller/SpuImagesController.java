package com.ww.mall.product.controller;

import com.ww.mall.product.service.SpuImagesService;
import com.ww.mall.product.entity.SpuImages;
import com.ww.mall.common.constant.R;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
* @author ww
* @since 2021-03-10
*/
@Slf4j
@Api(tags = "spu图片")
@RestController
@RequestMapping("spu-images")
public class SpuImagesController {

    @Autowired
    public SpuImagesService spuImagesService;

    @ApiOperation(value = "新增")
    @PostMapping("/save")
    public R save(@RequestBody SpuImages spuImages){
        spuImagesService.save(spuImages);
        return R.ok("保存成功");
    }

    @ApiOperation(value = "根据id删除")
    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        spuImagesService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation(value = "条件查询")
    @PostMapping("/get")
    public R list(@RequestBody SpuImages spuImages){
        List<SpuImages> spuImagesList = spuImagesService.list(new QueryWrapper<>(spuImages));
        return R.ok("查询成功").put("data",spuImagesList);
    }

    @ApiOperation(value = "条件列表（分页）")
    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody SpuImages spuImages){
        IPage<SpuImages> page = spuImagesService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(spuImages));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @ApiOperation(value = "详情")
    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        SpuImages spuImages = spuImagesService.getById(id);
        return R.ok("查询成功").put("data",spuImages);
    }

    @ApiOperation(value = "根据id修改")
    @PostMapping("/update")
    public R update(@RequestBody SpuImages spuImages){
        spuImagesService.updateById(spuImages);
        return R.ok("更新成功");
    }
}
