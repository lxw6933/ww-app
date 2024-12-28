package com.ww.mall.search;

/**
 * @author ww
 * @create 2024-07-16 23:01
 * @description:
 */
public class InsertSort {

    public static void main(String[] args) {

    }

    /**
     * @param arr                 数组
     * @param notSortedFirstIndex 未排好序的第一个元素下标，后面的元素都需要插入排序
     */
    public static void insertSort(int[] arr, int notSortedFirstIndex) {
        if (notSortedFirstIndex == arr.length) {
            return;
        }
        // 获取第一个需要插入排序的元素
        int t = arr[notSortedFirstIndex];
        // 获取排好序的最后【最大】一个的元素的下标
        int maxIndex = notSortedFirstIndex - 1;
        // 如果找到第一个比t小的元素，则完成
        while (maxIndex >= 0 && t < arr[maxIndex]) {
            // 交换位置
            arr[maxIndex + 1] = arr[maxIndex];
            // 将需要排序的元素下标，往前移，与下一个元素比较
            maxIndex--;
        }
        // 找到插入位置
        arr[maxIndex + 1] = t;
        insertSort(arr, notSortedFirstIndex);
    }

}
