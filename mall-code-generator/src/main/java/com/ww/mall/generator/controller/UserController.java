package com.ww.mall.generator.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.common.R;
import com.ww.mall.generator.entity.User;
import com.ww.mall.generator.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
* @author ww
* @since 2021-03-09
*/
@Slf4j
@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    public UserService userService;

    @PostMapping("/save")
    public R save(@RequestBody User user){
        userService.save(user);
        return R.ok("保存成功");
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        userService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/get")
    public R list(@RequestBody User user){
        List<User> userList = userService.list(new QueryWrapper<>(user));
        return R.ok("查询成功").put("data",userList);
    }

    @GetMapping("/list/{pageNum}/{pageSize}")
    public Object list(@PathVariable("pageNum")Long pageNum, @PathVariable("pageSize")Long pageSize){
        IPage<User> page = userService.page(new Page<>(pageNum, pageSize), null);
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
    }

    @GetMapping("/get/{id}")
    public R get(@PathVariable("id") Long id){
        User user = userService.getById(id);
        return R.ok("查询成功").put("data",user);
    }

    @PostMapping("/update/{id}")
    public R update(@PathVariable("id") Long id, @RequestBody User user){
        user.setId(id);
        userService.updateById(user);
        return R.ok("更新成功");
    }

    @PostMapping("/testUpdate")
    public R testUpdate() {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id","")
                .eq("","")
                .set("", "")
                .set("", "");
        boolean update = userService.update(null, wrapper);
        System.out.println("=====================");
        boolean update1 = userService.update(wrapper);
        return R.ok("ok");
    }

}
