package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.AttrAttrgroupRelation;
import com.ww.mall.product.service.AttrAttrgroupRelationService;
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
@RequestMapping("attr-attrgroup-relation")
public class AttrAttrgroupRelationController {

    @Autowired
    public AttrAttrgroupRelationService attrAttrgroupRelationService;

    @PostMapping("/save")
    public R save(@RequestBody AttrAttrgroupRelation attrAttrgroupRelation){
        attrAttrgroupRelationService.save(attrAttrgroupRelation);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        attrAttrgroupRelationService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody AttrAttrgroupRelation attrAttrgroupRelation){
        List<AttrAttrgroupRelation> attrAttrgroupRelationList = attrAttrgroupRelationService.list(new QueryWrapper<>(attrAttrgroupRelation));
        return R.ok("查询成功").put("data",attrAttrgroupRelationList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody AttrAttrgroupRelation attrAttrgroupRelation){
        IPage<AttrAttrgroupRelation> page = attrAttrgroupRelationService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(attrAttrgroupRelation));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        AttrAttrgroupRelation attrAttrgroupRelation = attrAttrgroupRelationService.getById(id);
        return R.ok("查询成功").put("data",attrAttrgroupRelation);
    }

    @PostMapping("/update")
    public R update(@RequestBody AttrAttrgroupRelation attrAttrgroupRelation){
        attrAttrgroupRelationService.updateById(attrAttrgroupRelation);
        return R.ok("更新成功");
    }
}
