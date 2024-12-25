package com.ww.mall.mybatis.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.common.constant.Constant;
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
    public static <T> List<T> queryByCursor(BaseMapper<T> mapper,
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
        return mapper.selectList(queryWrapper.last("LIMIT " + pageSize));
    }

    public static <T> List<T> queryByIdCursor(BaseMapper<T> mapper, QueryWrapper<T> queryWrapper, int pageSize, Long cursorValue) {
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
            List<A> aList = MybatisPlusUtils.queryByIdCursor(aMapper, queryWrapper, pageSize, cursorValue);
            // 处理数据
            aList.forEach(a -> System.out.println("A: " + a));
            // 更新游标值（用于下一页查询）
            if (!aList.isEmpty()) {
                cursorValue = aList.get(aList.size() - 1).getId();
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
