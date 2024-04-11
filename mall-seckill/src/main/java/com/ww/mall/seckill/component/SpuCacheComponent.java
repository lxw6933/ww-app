package com.ww.mall.seckill.component;

import com.ww.mall.seckill.manager.MallCacheManager;
import com.ww.mall.web.template.AbstractMallCacheTemplate;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-04-11- 16:08
 * @description:
 */
@Component
public class SpuCacheComponent extends AbstractMallCacheTemplate<Long, String> {

    @Override
    protected String secondCache(Long param) {
        return MallCacheManager.spuCache.get(param.toString(), res -> null);
    }

    @Override
    protected String firstCache(Long param) {
        return null;
    }

    @Override
    protected String databaseData(Long param) {
        return null;
    }

    @Override
    protected void setCache(String data) {

    }
}
