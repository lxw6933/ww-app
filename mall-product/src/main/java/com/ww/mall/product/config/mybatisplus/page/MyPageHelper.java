package com.ww.mall.product.config.mybatisplus.page;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.page.PageMethod;
import com.ww.mall.common.exception.ValidatorException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @description: PageHelper扩展
 * @author: ww
 * @create: 2021-05-12 18:56
 */
public class MyPageHelper extends PageHelper {
    public static final char UNDERLINE = '_';

    /** 最大每页记录数，避免过多导致内存溢出 */
    public static final int MAX_LIMIT = 2000;

    /** 最小每页记录数 */
    public static final int MIN_LIMIT = 1;

    /** 正序 */
    public static final String ASC = "asc";

    /** 倒序 */
    public static final String DESC = "desc";

    /**
     * 拓展PageHelper的startPage，对orderBy的值进行处理，有驼峰写法转成数据库字段下划线写法
     * @deprecated 注意：方法已过期，请使用以下两个方法，以避免排序字段不存在而导致系统繁忙的异常
     * @param pagination 分页信息
     * @param <E> 泛型
     * @return Page<E>
     */
    @Deprecated
    public static <E> Page<E> startPage(Pagination pagination) {
        if (pagination.getLimit() > MAX_LIMIT || pagination.getLimit() < MIN_LIMIT) {
            throw new ValidatorException("common.limit.error", MIN_LIMIT, MAX_LIMIT);
        }
        return PageMethod.startPage(pagination.getPage(), pagination.getLimit(), camelToUnderline(pagination.getOrderBy()));
    }

    /**
     * 拓展PageHelper的startPage，对orderBy的值进行处理，如发现排序字段不在类字段中，则忽略
     * @param pagination 分页信息
     * @param clazz class对象
     * @param <E> 泛型
     * @return Page<E>
     */
    public static <E> Page<E> startPage(Pagination pagination, Class<?> clazz) {
        return startPage(pagination, column -> {
            Class<?> clazz1 = clazz;
            for (; clazz1 != Object.class; clazz1 = clazz1.getSuperclass()) {
                Field[] fields = clazz1.getDeclaredFields();
                for (Field field : fields) {
                    if (Objects.equals(field.getName(), column)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /**
     * 拓展PageHelper的startPage，对orderBy的值进行处理，如发现排序字段不在类字段中，则忽略
     * @param pagination 分页信息
     * @param columns 字段名
     * @param <E> 泛型
     * @return Page<E>
     */
    public static <E> Page<E> startPage(Pagination pagination, String... columns) {
        return startPage(pagination, column -> {
            for (String col : columns) {
                if (Objects.equals(col, column)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 拓展PageHelper的startPage
     * @param pagination 分页信息
     * @param predicate  函数式接口，用于断定
     * @return Page<E>
     */
    private static <E> Page<E> startPage(Pagination pagination, Predicate<String> predicate) {
        if (pagination.getLimit() > MAX_LIMIT || pagination.getLimit() < MIN_LIMIT) {
            throw new ValidatorException("common.limit.error", MIN_LIMIT, MAX_LIMIT);
        }
        String orderBy = pagination.getOrderBy();
        if (StringUtils.isNotBlank(orderBy)) {
            boolean isValid;
            String[] array = StringUtils.split(orderBy, " ");
            String column = array[0];
            isValid = predicate.test(column);
            if (isValid) {
                if (array.length == 2) {
                    if (!StringUtils.equalsIgnoreCase(ASC, array[1]) && !StringUtils.equalsIgnoreCase(DESC, array[1])) {
                        throw new ValidatorException("common.sort.error");
                    }
                }
                return PageMethod.startPage(pagination.getPage(), pagination.getLimit(), camelToUnderline(pagination.getOrderBy()));
            }
        }
        return PageMethod.startPage(pagination.getPage(), pagination.getLimit());
    }

    /**
     * 驼峰转下划线
     * @param name name 字符串
     * @return String
     */
    public static String camelToUnderline(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        int len = name.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(UNDERLINE);
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
