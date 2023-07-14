package com.ww.mall.coupon;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.coupon.dao.UserMapper;
import com.ww.mall.coupon.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
class MallCouponApplicationTests {

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
