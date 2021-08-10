package com.ww.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.CommentReplay;
import com.ww.mall.product.service.CommentReplayService;
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
@RequestMapping("comment-replay")
public class CommentReplayController {

    @Autowired
    public CommentReplayService commentReplayService;

    @PostMapping("/save")
    public R save(@RequestBody CommentReplay commentReplay){
        commentReplayService.save(commentReplay);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        commentReplayService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody CommentReplay commentReplay){
        List<CommentReplay> commentReplayList = commentReplayService.list(new QueryWrapper<>(commentReplay));
        return R.ok("查询成功").put("data",commentReplayList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody CommentReplay commentReplay){
        IPage<CommentReplay> page = commentReplayService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(commentReplay));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        CommentReplay commentReplay = commentReplayService.getById(id);
        return R.ok("查询成功").put("data",commentReplay);
    }

    @PostMapping("/update")
    public R update(@RequestBody CommentReplay commentReplay){
        commentReplayService.updateById(commentReplay);
        return R.ok("更新成功");
    }
}
