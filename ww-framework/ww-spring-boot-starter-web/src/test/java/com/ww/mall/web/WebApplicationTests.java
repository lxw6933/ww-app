package com.ww.mall.web;

import cn.hutool.core.net.NetUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@SpringBootTest
class WebApplicationTests {

    @Test
    void fileTest() throws IOException {
//        Files.copy();
        Files.walkFileTree(Paths.get(""), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.out.println("进入文件目录：" + dir);
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("目录下文件：" + file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                System.out.println("遍历完文件后退出当前目录：" + dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    public static void main(String[] args) {
        String ip = "111.206.170.20";
        String a = "111.206.170.0/24";
        System.out.println(NetUtil.isInRange(ip, a));

//        String secret = "CITYEFKK16FU5UYNURQ2BLVRBVVKXPWW";
//        Map<String, Object> reqBO = new HashMap<>(128);
//        reqBO.put("jwOrderCode", "M132456456132");
//        reqBO.put("checkStatus", 1);
//        reqBO.put("checkPrice", "9527.88");
//        reqBO.put("checkDate", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
//        String reqStr = JSON.toJSONString(reqBO) + secret;
//        System.out.println("签名：" + DigestUtils.md5Hex(reqStr));
    }

}
