package com.ww.app.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * @author ww
 * @create 2024-09-22 18:36
 * @description:
 */
public class CollectionUtils {

    /**
     * true：匹配predicate到集合中任意一个元素
     */
    public static <T> boolean anyMatch(Collection<T> source, Predicate<T> predicate) {
        return source.stream().anyMatch(predicate);
    }

    /**
     * 求和
     */
    public static <T, V extends Comparable<? super V>> V getSumValue(List<T> source, Function<T, V> valueFunc, BinaryOperator<V> accumulator) {
        return getSumValue(source, valueFunc, accumulator, null);
    }

    public static <T, V extends Comparable<? super V>> V getSumValue(Collection<T> source, Function<T, V> valueFunc, BinaryOperator<V> accumulator, V defaultValue) {
        if (CollUtil.isEmpty(source)) {
            return defaultValue;
        }
        assert !source.isEmpty();
        return source.stream().map(valueFunc).filter(Objects::nonNull).reduce(accumulator).orElse(defaultValue);
    }

    /**
     * 最大值
     */
    public static <T, V extends Comparable<? super V>> V getMaxValue(Collection<T> source, Function<T, V> valueFunc) {
        if (CollUtil.isEmpty(source)) {
            return null;
        }
        assert !source.isEmpty();
        T t = source.stream().max(Comparator.comparing(valueFunc)).get();
        return valueFunc.apply(t);
    }

    /**
     * 最小值
     */
    public static <T, V extends Comparable<? super V>> V getMinValue(Collection<T> source, Function<T, V> valueFunc) {
        if (CollUtil.isEmpty(source)) {
            return null;
        }
        assert !source.isEmpty();
        T t = source.stream().min(Comparator.comparing(valueFunc)).get();
        return valueFunc.apply(t);
    }

    /**
     * 过滤集合
     */
    public static <T> List<T> filterList(Collection<T> source, Predicate<T> filter) {
        if (CollUtil.isEmpty(source)) {
            return new ArrayList<>();
        }
        return source.stream().filter(filter).collect(Collectors.toList());
    }

    /**
     * 数组对象===>获取target集合
     *
     * @param source 数组
     * @param func   target convert
     * @param <T>    数组元素类型
     * @param <R>    target类型
     * @return target集合
     */
    public static <T, R> List<R> convertList(T[] source, Function<T, R> func) {
        if (ArrayUtil.isEmpty(source)) {
            return new ArrayList<>();
        }
        return convertList(Arrays.asList(source), func);
    }

    public static <T, R> List<R> convertList(Collection<T> source, Function<T, R> func) {
        if (CollUtil.isEmpty(source)) {
            return new ArrayList<>();
        }
        return source.stream().map(func).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T, R> List<R> convertList(Collection<T> source, Function<T, R> func, Predicate<T> filter) {
        if (CollUtil.isEmpty(source)) {
            return new ArrayList<>();
        }
        return source.stream().filter(filter).map(func).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 将其他集合类型转成set
     *
     * @param source 集合
     * @param <T>    元素类型
     * @return Set
     */
    public static <T> Set<T> convertSet(Collection<T> source) {
        return convertSet(source, v -> v);
    }

    public static <T, R> Set<R> convertSet(Collection<T> source, Function<T, R> func) {
        if (CollUtil.isEmpty(source)) {
            return new HashSet<>();
        }
        return source.stream().map(func).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static <T, R> Set<R> convertSet(Collection<T> source, Function<T, R> func, Predicate<T> filter) {
        if (CollUtil.isEmpty(source)) {
            return new HashSet<>();
        }
        return source.stream().filter(filter).map(func).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static <T, U> Set<U> convertSetByFlatMap(Collection<T> source,
                                                    Function<T, ? extends Stream<? extends U>> func) {
        if (CollUtil.isEmpty(source)) {
            return new HashSet<>();
        }
        return source.stream().filter(Objects::nonNull).flatMap(func).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static <T, U, R> Set<R> convertSetByFlatMap(Collection<T> source,
                                                       Function<? super T, ? extends U> mapper,
                                                       Function<U, ? extends Stream<? extends R>> func) {
        if (CollUtil.isEmpty(source)) {
            return new HashSet<>();
        }
        return source.stream().map(mapper).filter(Objects::nonNull).flatMap(func).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * 将集合===>map [key：keyFunc, value：集合元素]
     *
     * @param source  集合
     * @param keyFunc key function
     * @param <T>     集合元素类型
     * @param <K>     key类型
     * @return map
     */
    public static <T, K> Map<K, T> convertMap(Collection<T> source, Function<T, K> keyFunc) {
        if (CollUtil.isEmpty(source)) {
            return new HashMap<>();
        }
        return convertMap(source, keyFunc, Function.identity());
    }

    public static <T, K> Map<K, T> convertMap(Collection<T> source, Function<T, K> keyFunc, Supplier<? extends Map<K, T>> supplier) {
        if (CollUtil.isEmpty(source)) {
            return supplier.get();
        }
        return convertMap(source, keyFunc, Function.identity(), supplier);
    }

    public static <T, K, V> Map<K, V> convertMap(Collection<T> source, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        if (CollUtil.isEmpty(source)) {
            return new HashMap<>();
        }
        return convertMap(source, keyFunc, valueFunc, (v1, v2) -> v1);
    }

    public static <T, K, V> Map<K, V> convertMap(Collection<T> source, Function<T, K> keyFunc, Function<T, V> valueFunc, BinaryOperator<V> mergeFunction) {
        if (CollUtil.isEmpty(source)) {
            return new HashMap<>();
        }
        return convertMap(source, keyFunc, valueFunc, mergeFunction, HashMap::new);
    }

    public static <T, K, V> Map<K, V> convertMap(Collection<T> source, Function<T, K> keyFunc, Function<T, V> valueFunc, Supplier<? extends Map<K, V>> supplier) {
        if (CollUtil.isEmpty(source)) {
            return supplier.get();
        }
        return convertMap(source, keyFunc, valueFunc, (v1, v2) -> v1, supplier);
    }

    public static <T, K, V> Map<K, V> convertMap(Collection<T> source, Function<T, K> keyFunc, Function<T, V> valueFunc, BinaryOperator<V> mergeFunction, Supplier<? extends Map<K, V>> supplier) {
        if (CollUtil.isEmpty(source)) {
            return new HashMap<>();
        }
        return source.stream().collect(Collectors.toMap(keyFunc, valueFunc, mergeFunction, supplier));
    }


    /**
     * 将一个集合以某个key进行分组， value是List<T>
     *
     * @param source  集合
     * @param keyFunc key分组规则
     * @param <T>     集合元素类型
     * @param <K>     key类型
     * @return Map<K, List < T>>
     */
    public static <T, K> Map<K, List<T>> convertGroupListMap(Collection<T> source, Function<T, K> keyFunc) {
        if (CollUtil.isEmpty(source)) {
            return new HashMap<>();
        }
        return source.stream().collect(Collectors.groupingBy(keyFunc, Collectors.mapping(t -> t, Collectors.toList())));
    }

    /**
     * 将一个集合以某个key进行分组， value是List<V>  V是将T进行 valueFunc 后的结果
     *
     * @param source  集合
     * @param keyFunc key分组规则
     * @param <T>     集合元素类型
     * @param <K>     key类型
     * @return Map<K, List < V>>
     */
    public static <T, K, V> Map<K, List<V>> convertGroupListMap(Collection<T> source, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        if (CollUtil.isEmpty(source)) {
            return new HashMap<>();
        }
        return source.stream().collect(Collectors.groupingBy(keyFunc, Collectors.mapping(valueFunc, Collectors.toList())));
    }

    public static <T, K, V> Map<K, Set<V>> convertGroupSetMap(Collection<T> source, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        if (CollUtil.isEmpty(source)) {
            return new HashMap<>();
        }
        return source.stream().collect(Collectors.groupingBy(keyFunc, Collectors.mapping(valueFunc, Collectors.toSet())));
    }

    /**
     * 对比老、新两个列表，找出新增、修改、删除的数据
     *
     * @param oldList  老列表
     * @param newList  新列表
     * @param sameFunc 对比函数，返回 true 表示相同，返回 false 表示不同
     *                 注意，same 是通过每个元素的“标识”，判断它们是不是同一个数据
     * @return [新增列表、修改列表、删除列表]
     */
    public static <T> List<List<T>> diff(Collection<T> oldList, Collection<T> newList, BiFunction<T, T, Boolean> sameFunc) {
        List<T> createList = new LinkedList<>(newList);
        List<T> updateList = new ArrayList<>();
        List<T> deleteList = new ArrayList<>();

        // 通过以 oldList 为主遍历，找出 updateList 和 deleteList
        for (T oldObj : oldList) {
            // 1. 寻找是否有匹配的
            T foundObj = null;
            for (Iterator<T> iterator = createList.iterator(); iterator.hasNext(); ) {
                T newObj = iterator.next();
                // 1.1 不匹配，则直接跳过
                if (!sameFunc.apply(oldObj, newObj)) {
                    continue;
                }
                // 1.2 匹配，则移除，并结束寻找
                iterator.remove();
                foundObj = newObj;
                break;
            }
            // 2. 匹配添加到 updateList；不匹配则添加到 deleteList 中
            if (foundObj != null) {
                updateList.add(foundObj);
            } else {
                deleteList.add(oldObj);
            }
        }
        return asList(createList, updateList, deleteList);
    }

}
