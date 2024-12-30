package com.ww.app.seckill;

import com.ww.app.minio.MinioTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;

@SpringBootTest
class SeckillServerApplicationTests {

    @Resource
    private MinioTemplate minioTemplate;

    @Test
    void removeBucket() {
//        System.out.println("创建bucket" + mallMinioTemplate.createBucket("ww-demo"));
        String filePath = "C://Users/16143/Desktop/temp/hdfs-site.xml";
        try (FileInputStream in = new FileInputStream(filePath)) {
            minioTemplate.upload(in, "ww-demo", "/f1/a.xml");
        } catch (Exception e) {
            System.out.println("上传文件异常");
        }

//        System.out.println("列表bucket" + mallMinioTemplate.listBucketAllFile("ww-demo", true));
//        System.out.println("删除bucket" + mallMinioTemplate.removeBucket("ww-demo"));
    }

}
