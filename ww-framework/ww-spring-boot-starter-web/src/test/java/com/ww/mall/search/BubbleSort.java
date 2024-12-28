package com.ww.mall.search;

/**
 * @author ww
 * @create 2024-07-16 22:30
 * @description:
 */
public class BubbleSort {

    public static void main(String[] args) {
        int[] arr = new int[]{5, 9, 2, 4, 1, 3};
        int[] arr2 = new int[]{5, 9, 2, 4, 1, 3};
        bubbleSort(arr, arr.length - 1);
        for (int e : arr) {
            System.out.println(e);
        }
        bubbleSort2(arr2, arr2.length - 1);
        for (int e : arr2) {
            System.out.println(e);
        }
    }

    public static void bubbleSort(int[] arr, int length) {
        if (length == 0) {
            return;
        }
        for (int i = 0; i < length; i++) {
            if (arr[i] > arr[i + 1]) {
                int temp = arr[i];
                arr[i] = arr[i + 1];
                arr[i + 1] = temp;
            }
        }
        bubbleSort(arr, length - 1);
    }

    public static void bubbleSort2(int[] arr, int length) {
        if (length == 0) {
            return;
        }
        int x = 0;
        for (int i = 0; i < length; i++) {
            if (arr[i] > arr[i + 1]) {
                int temp = arr[i];
                arr[i] = arr[i + 1];
                arr[i + 1] = temp;
                x = i;
            }
        }
        bubbleSort(arr, x);
    }

}
