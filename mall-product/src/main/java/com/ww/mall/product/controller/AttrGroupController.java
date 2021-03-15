package com.ww.mall.product.controller;

import cn.hutool.core.util.StrUtil;
import com.ww.mall.product.service.AttrGroupService;
import com.ww.mall.product.entity.AttrGroup;
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
@Api(tags = "属性分组")
@RestController
@RequestMapping("attr-group")
public class AttrGroupController {

    @Autowired
    public AttrGroupService attrGroupService;

    @ApiOperation(value = "新增")
    @PostMapping("/save")
    public R save(@RequestBody AttrGroup attrGroup){
        attrGroupService.save(attrGroup);
        return R.ok("保存成功");
    }

    @ApiOperation(value = "根据id删除")
    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        attrGroupService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation(value = "根据id批量删除")
    @PostMapping("/batchDelete")
    public R batchDelete(@RequestBody List<Long> ids){
        attrGroupService.removeByIds(ids);
        return R.ok("批量删除成功");
    }

//    @ApiOperation(value = "条件查询")
//    @PostMapping("/get")
//    public R list(@RequestBody AttrGroup attrGroup){
//        List<AttrGroup> attrGroupList = attrGroupService.list(new QueryWrapper<>(attrGroup));
//        return R.ok("查询成功").put("data",attrGroupList);
//    }

    @ApiOperation(value = "条件查询")
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

    @ApiOperation(value = "条件列表（分页）")
    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody AttrGroup attrGroup){
        IPage<AttrGroup> page = attrGroupService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(attrGroup));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @ApiOperation(value = "详情")
    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        AttrGroup attrGroup = attrGroupService.getById(id);
        return R.ok("查询成功").put("data",attrGroup);
    }

    @ApiOperation(value = "根据id修改")
    @PostMapping("/update")
    public R update(@RequestBody AttrGroup attrGroup){
        attrGroupService.updateById(attrGroup);
        return R.ok("更新成功");
    }
}
