package com.ww.mall.seckill;

import com.ww.mall.minio.MallMinioTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;

@SpringBootTest
class MallSeckillApplicationTests {

    @Resource
    private MallMinioTemplate mallMinioTemplate;

    @Test
    void removeBucket() {
//        System.out.println("创建bucket" + mallMinioTemplate.createBucket("mall-demo"));
        String filePath = "C://Users/16143/Desktop/temp/hdfs-site.xml";
        try (FileInputStream in = new FileInputStream(filePath)) {
            mallMinioTemplate.upload(in, "mall-demo", "/f1/a.xml");
        } catch (Exception e) {
            System.out.println("上传文件异常");
        }

//        System.out.println("列表bucket" + mallMinioTemplate.listBucketAllFile("mall-demo", true));
//        System.out.println("删除bucket" + mallMinioTemplate.removeBucket("mall-demo"));
    }

}
