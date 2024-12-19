package com.ww.mall.mybatis.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.utils.CommonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-19- 11:35
 * @description: mysql游标分页
 */
@Slf4j
public class MybatisPlusUtils {

    /**
     * 通用游标分页查询方法
     *
     * @param mapper       MyBatis-Plus 的 BaseMapper 实例
     * @param queryWrapper 查询条件封装器
     * @param pageSize     每页大小
     * @param cursorField  游标字段（通常为主键或唯一索引字段）
     * @param cursorValue  游标值（初始值可为 null 表示从头开始查询）
     * @param <T>          实体类型
     * @return 分页结果
     */
    public static <T> Page<T> queryByCursor(BaseMapper<T> mapper,
                                            QueryWrapper<T> queryWrapper,
                                            int pageSize,
                                            String cursorField,
                                            Object cursorValue) {
        // 如果游标值不为空，则添加大于条件
        if (cursorValue != null) {
            queryWrapper.gt(cursorField, cursorValue);
        }

        // 添加排序条件（游标字段升序）
        queryWrapper.orderByAsc(cursorField);

        // 查询数据
        List<T> records = mapper.selectList(queryWrapper.last("LIMIT " + pageSize));

        // 构造分页结果
        Page<T> page = new Page<>();
        page.setRecords(records);
        page.setSize(pageSize);
        // 当前页（游标分页意义上无实际页码）
        page.setCurrent(cursorValue == null ? 1 : -1);

        // 设置下一次查询的游标值
        if (!records.isEmpty()) {
            Object lastCursorValue = CommonUtils.getCursorValue(records, cursorField);
            page.setTotal(records.size());
            page.setSearchCount(false);
            // 添加升序排序信息
            page.addOrder(OrderItem.asc(cursorField));
            log.info("Next cursor value: {}", lastCursorValue);
        }
        return page;
    }

    public static <T> Page<T> queryByIdCursor(BaseMapper<T> mapper, QueryWrapper<T> queryWrapper, int pageSize, Long cursorValue) {
        return queryByCursor(mapper, queryWrapper, pageSize, Constant.MYSQL_PRIMARY_KEY, cursorValue);
    }

    public static void main(String[] args) {
        int pageSize = 10;
        Long cursorValue = null;

        // 创建查询条件
        QueryWrapper<A> queryWrapper = new QueryWrapper<>();

        BaseMapper<A> aMapper = null;

        // 游标分页查询
        do {
            Page<A> page = MybatisPlusUtils.queryByIdCursor(aMapper, queryWrapper, pageSize, cursorValue);

            // 处理数据
            page.getRecords().forEach(a -> System.out.println("A: " + a));

            // 更新游标值（用于下一页查询）
            if (!page.getRecords().isEmpty()) {
                cursorValue = page.getRecords().get(page.getRecords().size() - 1).getId();
            } else {
                break;
            }
        } while (true);
    }

    @Data
    static class A {
        private Long id;
    }

}
