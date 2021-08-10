package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.Attr;
import com.ww.mall.product.service.AttrService;
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
@RequestMapping("attr")
public class AttrController {

    @Autowired
    public AttrService attrService;

    @PostMapping("/save")
    public R save(@RequestBody Attr attr){
        attrService.save(attr);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        attrService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody Attr attr){
        List<Attr> attrList = attrService.list(new QueryWrapper<>(attr));
        return R.ok("查询成功").put("data",attrList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody Attr attr){
        IPage<Attr> page = attrService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(attr));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        Attr attr = attrService.getById(id);
        return R.ok("查询成功").put("data",attr);
    }

    @PostMapping("/update")
    public R update(@RequestBody Attr attr){
        attrService.updateById(attr);
        return R.ok("更新成功");
    }
}
