package com.ww.mall.config.mybatisplus.page;

import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description: 分页扩展类
 * @author: ww
 * @create: 2021-04-16 13:43
 */
public class MyPageInfo<T> extends PageInfo<T> {

    public MyPageInfo() {}

    public MyPageInfo(List<T> list) {
        super(list, 8);
    }

    public static <T> MyPageInfo<T> of(List<T> list) {
        return new MyPageInfo<>(list);
    }

    /**
     * 分页 entity 转换 VO
     * @param mapper mapper
     * @param <R> R
     * @return UWPageInfo
     */
    public <R> MyPageInfo<R> convert(Function<? super T, ? extends R> mapper) {
        MyPageInfo<R> pageInfo = new MyPageInfo<>();
        List<R> collect = this.getList().stream().map(mapper).collect(Collectors.toList());
        pageInfo.setList(collect);
        pageInfo.setTotal(this.getTotal());
        pageInfo.setPageNum(this.getPageNum());
        pageInfo.setPageSize(this.getPageSize());
        pageInfo.setPages(this.getPages());
        pageInfo.setSize(this.getSize());
        pageInfo.setStartRow(this.getStartRow());
        pageInfo.setEndRow(this.getEndRow());
        return pageInfo;
    }

}
