package com.ww.mall.product.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.product.entity.AttrGroup;
import com.ww.mall.product.service.AttrGroupService;
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
@RequestMapping("attr-group")
public class AttrGroupController {

    @Autowired
    public AttrGroupService attrGroupService;

    @PostMapping("/save")
    public R save(@RequestBody AttrGroup attrGroup){
        attrGroupService.save(attrGroup);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        attrGroupService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/batchDelete")
    public R batchDelete(@RequestBody List<Long> ids){
        attrGroupService.removeByIds(ids);
        return R.ok("批量删除成功");
    }

//    @PostMapping("/get")
//    public R list(@RequestBody AttrGroup attrGroup){
//        List<AttrGroup> attrGroupList = attrGroupService.list(new QueryWrapper<>(attrGroup));
//        return R.ok("查询成功").put("data",attrGroupList);
//    }

    @GetMapping("/list/{catelogId}")
    public R list(@PathVariable(value = "catelogId") Long catelogId,
                  @RequestParam(value = "key",required = false) String key) {
        List<AttrGroup> attrGroupList = null;
        QueryWrapper<AttrGroup> wrapper = new QueryWrapper<>();
        if(catelogId == 0){
            if(StrUtil.isNotEmpty(key)){
                wrapper.and((res) -> {
                    res.like("attr_group_name",key);
                });
            }
            attrGroupList = attrGroupService.list(wrapper);
        }else{
            wrapper.eq("catelog_id",catelogId);
            if(StrUtil.isNotEmpty(key)){
                wrapper.and((res) -> {
                    res.like("attr_group_name",key);
                });
            }
            attrGroupList = attrGroupService.list(wrapper);
        }
        return R.ok("查询成功").put("data",attrGroupList);
    }

    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody AttrGroup attrGroup){
        IPage<AttrGroup> page = attrGroupService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(attrGroup));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        AttrGroup attrGroup = attrGroupService.getById(id);
        return R.ok("查询成功").put("data",attrGroup);
    }

    @PostMapping("/update")
    public R update(@RequestBody AttrGroup attrGroup){
        attrGroupService.updateById(attrGroup);
        return R.ok("更新成功");
    }
}
