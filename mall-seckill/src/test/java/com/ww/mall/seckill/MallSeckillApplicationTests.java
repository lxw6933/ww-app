package com.ww.mall.seckill;

import com.ww.mall.minio.MallMinioTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class MallSeckillApplicationTests {

    @Resource
    private MallMinioTemplate mallMinioTemplate;

    @Test
    void removeBucket() {
        System.out.println("创建bucket" + mallMinioTemplate.createBucket("mall-demo"));
        System.out.println("列表bucket" + mallMinioTemplate.listBucketAllFile("mall-demo", true));
        System.out.println("删除bucket" + mallMinioTemplate.removeBucket("mall-demo"));
    }

}
