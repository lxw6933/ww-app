package com.ww.mall.product.controller;

import com.ww.mall.common.constant.RedisKey;
import com.ww.mall.common.utils.RedisUtils;
import com.ww.mall.product.service.CategoryService;
import com.ww.mall.product.entity.Category;
import com.ww.mall.common.constant.R;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
* @author ww
* @since 2021-03-10
*/
@Slf4j
@Api(tags = "商品三级分类")
@RestController
@RequestMapping("category")
public class CategoryController {

    @Autowired
    CategoryService categoryService;
    @Autowired
    RedisUtils redisUtils;
    @Autowired
    RedissonClient redisson;

    @ApiOperation(value = "新增")
    @PostMapping("/save")
    public R save(@RequestBody Category category){
        categoryService.save(category);
        return R.ok("保存成功");
    }

    @ApiOperation(value = "根据id删除")
    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        categoryService.removeById(id);
        return R.ok("删除成功");
    }

    @ApiOperation(value = "条件查询")
    @PostMapping("/get")
    public R list(@RequestBody Category category){
        List<Category> categoryList = categoryService.list(new QueryWrapper<>(category));
        return R.ok("查询成功").put("data",categoryList);
    }

    @ApiOperation(value = "条件列表（分页）")
    @PostMapping("/page/{pageNum}/{pageSize}")
    public Object page(@PathVariable("pageNum")Long pageNum,
                       @PathVariable("pageSize")Long pageSize,
                       @RequestBody Category category){
        IPage<Category> page = categoryService.page(new Page<>(pageNum, pageSize), new QueryWrapper<>(category));
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @ApiOperation(value = "详情")
    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        Category category = categoryService.getById(id);
        return R.ok("查询成功").put("data",category);
    }

    @ApiOperation(value = "根据id修改")
    @PostMapping("/update")
    public R update(@RequestBody Category category){
        categoryService.updateById(category);
        return R.ok("更新成功");
    }

    @GetMapping("/list/tree")
    public R listWithTree(){
        List<Category> categories = (List<Category>) redisUtils.get(RedisKey.PRODUCT_CATEGORY_DATA);
        if(categories == null){
            categories = categoryService.listWithTree();
            redisUtils.set(RedisKey.PRODUCT_CATEGORY_DATA,categories);
        }
        return R.ok("查询成功").put("data",categories);
    }

    @GetMapping("/redisson/{id}")
    public R redisson(@PathVariable("id") Long id) {
        RLock lock = redisson.getLock("demo");
        lock.lock();
        System.out.println(id+"获取到锁");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            System.out.println(id+"释放锁");
            lock.unlock();
        }
        return R.ok("OK");
    }

    @GetMapping("/read/{id}")
    public R read(@PathVariable("id") Long id) {
        RReadWriteLock lock = redisson.getReadWriteLock("test");
        lock.readLock().lock();
        System.out.println(id+"获取到读锁");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            System.out.println(id+"释放读锁");
            lock.readLock().unlock();
        }
        return R.ok("读OK");
    }

    @GetMapping("/write/{id}")
    public R write(@PathVariable("id") Long id) {
        RReadWriteLock lock = redisson.getReadWriteLock("test");
        lock.writeLock().lock();
        System.out.println(id+"获取到写锁");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            System.out.println(id+"释放写锁");
            lock.writeLock().unlock();
        }
        return R.ok("写OK");
    }



}
