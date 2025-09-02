package com.ww.app.admin.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ww
 * @create 2024-05-20- 09:55
 * @description:
 */
@Getter
@AllArgsConstructor
public enum SysMenuType implements IEnum<String> {

    CATALOG("catalog", "目录"),
    MENU("menu", "菜单"),
    EMBEDDED("embedded", "按钮"),
    LINK("link", "内嵌"),
    BUTTON("button", "外链");

    @JsonValue
    private final String value;
    private final String desc;

}
