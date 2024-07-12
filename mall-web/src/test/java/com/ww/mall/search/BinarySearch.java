package com.ww.mall.search;

/**
 * @author ww
 * @create 2024-07-12 22:26
 * @description:
 */
public class BinarySearch {

    public static void main(String[] args) {
        int[] nums = new int[]{1, 2, 3, 4, 5, 6, 7};
        int target = 7;
        System.out.println(binarySearch(nums, target));
    }

    public static int binarySearch(int[] nums, int target) {
        int i = 0;
        int j = nums.length - 1;
        while (i <= j) {
//            int mid = (i + j) / 2;
            // 无符号右移，防止最高位为1，成为负数
            int mid = (i + j) >>> 1;
            if (target < nums[mid]) {
                j = mid - 1;
            } else if (nums[mid] < target) {
                i = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
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


}
