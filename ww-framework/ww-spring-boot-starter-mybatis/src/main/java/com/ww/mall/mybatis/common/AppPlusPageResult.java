package com.ww.mall.mybatis.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ww.mall.common.common.AppPageResult;

import java.util.function.Function;

/**
 * @author ww
 * @create 2023-07-19- 13:48
 * @description:
 */
public class AppPlusPageResult<T, P> extends AppPageResult<T> {

    public AppPlusPageResult(IPage<T> page) {
        super((int) page.getCurrent(), (int) page.getSize(), (int) page.getTotal(), page.getRecords());
    }

    public AppPlusPageResult(IPage<P> page, Function<P, T> convert) {
        super((int) page.getCurrent(), (int) page.getSize(), (int) page.getTotal(), page.getRecords(), convert);
    }

}
