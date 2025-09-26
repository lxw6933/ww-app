package com.ww.app.ip.common;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ww.app.ip.enums.AreaTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-25 15:42
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
// 避免死循环 参见 https://gitee.com/yudaocode/yudao-cloud-mini/pulls/2 原因
@ToString(exclude = {"parent"})
public class Area {

    /**
     * 编号 - 全球，即根目录
     */
    public static final Integer ID_GLOBAL = 0;

    /**
     * 编号 - 中国
     */
    public static final Integer ID_CHINA = 1;

    /**
     * 编号
     */
    private Integer id;

    /**
     * 名字
     */
    private String name;

    /**
     * 类型
     * <p>
     * 枚举 {@link AreaTypeEnum}
     */
    private Integer type;

    /**
     * 父节点
     */
    @JsonManagedReference
    private Area parent;

    /**
     * 子节点
     */
    @JsonBackReference
    private List<Area> children;

}
