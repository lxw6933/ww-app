package com.ww.mall.seckill.service;

import com.ww.mall.seckill.view.bo.SensitiveWordBO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-05-14 23:23
 * @description:
 */
public interface DemoService {

    boolean seckillOrder();

    void traceId();

    void msg();

    void cache(String msg);

    void boomFilter();

    void liteFlow();

    String sensitiveWord(SensitiveWordBO content);

    void importData(MultipartFile file);

    void exportDate(HttpServletResponse response);
}
