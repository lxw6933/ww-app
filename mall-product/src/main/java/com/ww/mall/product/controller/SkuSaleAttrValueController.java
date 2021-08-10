package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.SkuSaleAttrValue;
import com.ww.mall.product.service.SkuSaleAttrValueService;
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
@RequestMapping("sku-sale-attr-value")
public class SkuSaleAttrValueController {

    @Autowired
    public SkuSaleAttrValueService skuSaleAttrValueService;

    @PostMapping("/save")
    public R save(@RequestBody SkuSaleAttrValue skuSaleAttrValue){
        skuSaleAttrValueService.save(skuSaleAttrValue);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        skuSaleAttrValueService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody SkuSaleAttrValue skuSaleAttrValue){
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueService.list(new QueryWrapper<>(skuSaleAttrValue));
        return R.ok("查询成功").put("data",skuSaleAttrValueList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody SkuSaleAttrValue skuSaleAttrValue){
        IPage<SkuSaleAttrValue> page = skuSaleAttrValueService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(skuSaleAttrValue));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueService.getById(id);
        return R.ok("查询成功").put("data",skuSaleAttrValue);
    }

    @PostMapping("/update")
    public R update(@RequestBody SkuSaleAttrValue skuSaleAttrValue){
        skuSaleAttrValueService.updateById(skuSaleAttrValue);
        return R.ok("更新成功");
    }
}
