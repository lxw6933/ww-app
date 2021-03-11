package com.ww.mall.product.controller;

import com.ww.mall.product.service.SpuCommentService;
import com.ww.mall.product.entity.SpuComment;
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
@Api(tags = "商品评价")
@RestController
@RequestMapping("spu-comment")
public class SpuCommentController {

    @Autowired
    public SpuCommentService spuCommentService;

    @ApiOperation(value = "新增")
    @PostMapping("/save")
    public R save(@RequestBody SpuComment spuComment){
        spuCommentService.save(spuComment);
        return R.ok("保存成功");
    }

    @ApiOperation(value = "根据id删除")
    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        spuCommentService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation(value = "条件查询")
    @PostMapping("/get")
    public R list(@RequestBody SpuComment spuComment){
        List<SpuComment> spuCommentList = spuCommentService.list(new QueryWrapper<>(spuComment));
        return R.ok("查询成功").put("data",spuCommentList);
    }

    @ApiOperation(value = "条件列表（分页）")
    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody SpuComment spuComment){
        IPage<SpuComment> page = spuCommentService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(spuComment));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @ApiOperation(value = "详情")
    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        SpuComment spuComment = spuCommentService.getById(id);
        return R.ok("查询成功").put("data",spuComment);
    }

    @ApiOperation(value = "根据id修改")
    @PostMapping("/update")
    public R update(@RequestBody SpuComment spuComment){
        spuCommentService.updateById(spuComment);
        return R.ok("更新成功");
    }
}
