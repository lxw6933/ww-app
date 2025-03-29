package com.ww.app.common.enums;

/**
 * @author ww
 * @create 2025-03-29- 15:45
 * @description: 枚举基类
 */
public interface BaseEnum {

    /**
     * 获取枚举显示值（用于枚举convert）
     */
    String getShowValue();

    /**
     * 根据值获取枚举实例
     */
    static <E extends BaseEnum> E fromShowValue(Class<E> enumClass, String value) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getShowValue().equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("No enum constant " + enumClass.getCanonicalName() + " for value '" + value + "'");
    }

}
