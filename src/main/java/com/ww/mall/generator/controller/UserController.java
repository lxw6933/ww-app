package com.ww.mall.generator.controller;



import com.ww.mall.generator.service.UserService;
import com.ww.mall.generator.entity.User;
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
* <p>
    *  前端控制器
    * </p>
*
* @author ww
* @since 2021-03-09
*/

@Slf4j
@Api(tags = "")
    @RestController
@RequestMapping("//user")
        public class UserController {

        @Autowired
        public UserService userService;

        @ApiOperation(value = "新增")
        @PostMapping("/save")
        public R save(@RequestBody User user){
        userService.save(user);
        return R.ok("保存成功");
        }

        @ApiOperation(value = "根据id删除")
        @PostMapping("/delete/{id}")
        public R delete(@PathVariable("id") Long id){
        userService.removeById(id);
        return R.ok("删除成功");
        }

        @ApiOperation(value = "条件查询")
        @PostMapping("/get")
        public R list(@RequestBody User user){
        List<User> userList = userService.list(new QueryWrapper<>(user));
        return R.ok("查询成功").put("data",userList);
        }

        @ApiOperation(value = "列表（分页）")
        @GetMapping("/list/{pageNum}/{pageSize}")
        public Object list(@PathVariable("pageNum")Long pageNum, @PathVariable("pageSize")Long pageSize){
        IPage<User> page = userService.page(
        new Page<>(pageNum, pageSize), null);
        return R.ok("查询成功").put("data",page.getRecords()).put("total",page.getTotal());
        }

        @ApiOperation(value = "详情")
        @GetMapping("/get/{id}")
        public R get(@PathVariable("id") String id){
        User user = userService.getById(id);
        return R.ok("查询成功").put("data",user);
        }

        @ApiOperation(value = "根据id修改")
        @PostMapping("/update/{id}")
        public R update(@PathVariable("id") String id, @RequestBody User user){
        user.setId(id);
        userService.updateById(user);
        return R.ok("更新成功");
        }


    }
