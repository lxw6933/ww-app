package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.SpuInfoDesc;
import com.ww.mall.product.service.SpuInfoDescService;
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
@RequestMapping("spu-info-desc")
public class SpuInfoDescController {

    @Autowired
    public SpuInfoDescService spuInfoDescService;

    @PostMapping("/save")
    public R save(@RequestBody SpuInfoDesc spuInfoDesc){
        spuInfoDescService.save(spuInfoDesc);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        spuInfoDescService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody SpuInfoDesc spuInfoDesc){
        List<SpuInfoDesc> spuInfoDescList = spuInfoDescService.list(new QueryWrapper<>(spuInfoDesc));
        return R.ok("查询成功").put("data",spuInfoDescList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody SpuInfoDesc spuInfoDesc){
        IPage<SpuInfoDesc> page = spuInfoDescService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(spuInfoDesc));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        SpuInfoDesc spuInfoDesc = spuInfoDescService.getById(id);
        return R.ok("查询成功").put("data",spuInfoDesc);
    }

    @PostMapping("/update")
    public R update(@RequestBody SpuInfoDesc spuInfoDesc){
        spuInfoDescService.updateById(spuInfoDesc);
        return R.ok("更新成功");
    }
}
