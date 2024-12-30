package com.ww.app.seckill.run;

import com.ww.app.seckill.manager.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-04-11- 09:23
 * @description:
 */
@Slf4j
@Component
public class SystemCacheRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始加载系统缓存...");
        for (int i = 0; i < 10; i++) {
            CacheManager.spuCache.put("spu" + i, "data" + i);
        }
        log.info("结束加载系统缓存...");
    }

}
