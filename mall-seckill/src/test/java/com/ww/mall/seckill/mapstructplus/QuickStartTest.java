package com.ww.mall.seckill.mapstructplus;

import cn.hutool.core.date.TimeInterval;
import com.ww.mall.seckill.service.DemoService;
import io.github.linpeilie.Converter;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author ww
 * @create 2024-11-04- 16:59
 * @description:
 */
@SpringBootTest
public class QuickStartTest {

    @Autowired
    private Converter converter;

    @Autowired
    private DemoService demoService;

    @Test
    public void test() {
        User user = new User();
        user.setUsername("jack");
        user.setAge(23);
        user.setYoung(false);

        UserDto userDto = converter.convert(user, UserDto.class);
        System.out.println(userDto);

        assert user.getUsername().equals(userDto.getUsername());
        assert user.getAge() == userDto.getAge();
        assert user.isYoung() == userDto.isYoung();

        User newUser = converter.convert(userDto, User.class);

        System.out.println(newUser);

        assert user.getUsername().equals(newUser.getUsername());
        assert user.getAge() == newUser.getAge();
        assert user.isYoung() == newUser.isYoung();
    }

    @Test
    public void test2() {
        TimeInterval timer = new TimeInterval();
        timer.start("Mapstruct-plus");
        demoService.testBeanCopy(0);
        System.out.println("Mapstruct-plus 耗时：" + timer.intervalSecond("Mapstruct-plus") + "秒");

        timer.start("BeanUtil");
        demoService.testBeanCopy(1);
        System.out.println("BeanUtil 耗时：" + timer.intervalSecond("BeanUtil") + "秒");
    }

    @Data
    public static class User {
        private String username;
        private int age;
        private boolean young;
    }

    @Data
    public static class UserDto {
        private String username;
        private int age;
        private boolean young;
    }

}
