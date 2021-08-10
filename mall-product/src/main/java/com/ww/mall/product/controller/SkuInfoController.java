package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.SkuInfo;
import com.ww.mall.product.service.SkuInfoService;
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
@RequestMapping("sku-info")
public class SkuInfoController {

    @Autowired
    public SkuInfoService skuInfoService;

    @PostMapping("/save")
    public R save(@RequestBody SkuInfo skuInfo){
        skuInfoService.save(skuInfo);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        skuInfoService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody SkuInfo skuInfo){
        List<SkuInfo> skuInfoList = skuInfoService.list(new QueryWrapper<>(skuInfo));
        return R.ok("查询成功").put("data",skuInfoList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody SkuInfo skuInfo){
        IPage<SkuInfo> page = skuInfoService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(skuInfo));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        SkuInfo skuInfo = skuInfoService.getById(id);
        return R.ok("查询成功").put("data",skuInfo);
    }

    @PostMapping("/update")
    public R update(@RequestBody SkuInfo skuInfo){
        skuInfoService.updateById(skuInfo);
        return R.ok("更新成功");
    }
}
