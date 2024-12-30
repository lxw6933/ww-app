package com.ww.app.search;

import java.util.Arrays;

/**
 * @author ww
 * @create 2024-07-12 22:26
 * @description:
 */
public class BinarySearch {

    public static void main(String[] args) {
        int[] nums = new int[]{1, 5, 5, 13, 13, 14, 14, 14, 25, 25, 25, 25, 36, 47};
        int target = 5;
        System.out.println(binarySearch(nums, target));
        System.out.println(binarySearchLeftMost(nums, target));
        System.out.println(binarySearchLeftMost2(nums, target));
//        System.out.println(binarySearch2(nums, target));
//        System.out.println(binarySearch3(nums, target));
        System.out.println(binarySearch4(nums, target));
    }

    public static int binarySearch(int[] nums, int target) {
        return binarySearch(nums, target, false);
    }

    public static int binarySearchLeftMost(int[] nums, int target) {
        return binarySearch(nums, target, true);
    }

    public static int binarySearchLeftMost2(int[] nums, int target) {
        int i = 0;
        int j = nums.length - 1;
        while(i <= j) {
            int mid = (i + j) >>> 1;
            if (target < nums[mid]) {
                j = mid - 1;
            } else {
                i = mid + 1;
            }
        }
        return i;
    }

    public static int binarySearch(int[] nums, int target, boolean leftMost) {
        int i = 0;
        int j = nums.length - 1;
        int candidate = -1;
        while (i <= j) {
//            int mid = (i + j) / 2;
            // 无符号右移，防止最高位为1，成为负数
            int mid = (i + j) >>> 1;
            if (target < nums[mid]) {
                j = mid - 1;
            } else if (nums[mid] < target) {
                i = mid + 1;
            } else {
                if (leftMost) {
                    candidate = mid;
                    j = mid - 1;
                } else {
                    return mid;
                }
            }
        }
        return candidate;
    }

    public static int binarySearch2(int[] nums, int target) {
        int i = 0;
        // j：不需要比较的下标
        int j = nums.length - 1;
        // 不能用= 否则查询不存在的target会陷入死循环
        while (i < j) {
            int mid = (i + j) >>> 1;
            if (target < nums[mid]) {
                j = mid;
            } else if (nums[mid] < target) {
                i = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    public static int binarySearch3(int[] nums, int target) {
        int i = 0;
        int j = nums.length;
        while (1 < j - i) {
            int mid = (i + j) >>> 1;
            if (target < nums[mid]) {
                j = mid;
            } else {
                i = mid;
            }
        }
        // 最好最差的时间复杂度都是一样的O(log2n)
        if (nums[i] == target) {
            return i;
        } else {
            return -1;
        }
    }

    public static int binarySearch4(int[] nums, int target) {
        // java
        int i = Arrays.binarySearch(nums, target);
        if (i < 0) {
            // +1 为了区分 -0 和 0
            int insertIndex = Math.abs(i + 1);
        }
        return i;
    }

}
