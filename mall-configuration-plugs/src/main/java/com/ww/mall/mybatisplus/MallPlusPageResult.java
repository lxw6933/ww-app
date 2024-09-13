package com.ww.mall.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ww.mall.common.common.MallPageResult;

import java.util.function.Function;

/**
 * @author ww
 * @create 2023-07-19- 13:48
 * @description:
 */
public class MallPlusPageResult<T, P> extends MallPageResult<T> {

    public MallPlusPageResult(IPage<T> page) {
        super((int) page.getCurrent(), (int) page.getSize(), (int) page.getTotal(), page.getRecords());
    }

    public MallPlusPageResult(IPage<P> page, Function<P, T> convert) {
        super((int) page.getCurrent(), (int) page.getSize(), (int) page.getTotal(), page.getRecords(), convert);
    }

}
