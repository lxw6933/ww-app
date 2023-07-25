package com.ww.mall.coupon;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class CouponApplicationTests {

    @Resource
    private UserMapper userMapper;

    @Test
    public void testSelect() {
        System.out.println(("----- selectAll method test ------"));
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        Page<User> page = new Page(2,2);
//        List<User> userList = userMapper.selectList(null);
        Page<User> userPageList = userMapper.selectPage(page, null);
//        Assert.assertEquals(2, userList.size());
//        userList.forEach(System.out::println);
        userPageList.getRecords().forEach(System.out::println);
    }

    @Test
    public void save(){
        User user = new User();
        user.setName("大白2");
        userMapper.insert(user);
    }


}
