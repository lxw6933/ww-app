package com.ww.app.admin.view.form;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ww.app.admin.enums.SysMenuType;
import com.ww.app.common.valid.group.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * @author ww
 * @create 2024-05-21- 13:32
 */
@Data
@Schema(description = "新增修改菜单权限表单")
public class SysMenuForm {

    /**
     * 用户ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    @Schema(description = "菜单权限id")
    private Long id;

    /**
     * 菜单类型
     */
    @Schema(description = "菜单类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private SysMenuType type;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 父级编号
     */
    @Schema(description = "父级编号")
    private Long pid;

    /**
     * URL地址
     */
    @Schema(description = "URL地址")
    @JsonAlias({"url", "path"})
    private String url;

    @Schema(description = "重定向地址")
    private String redirect;

    private Boolean valid;

    /**
     * 图标
     */
    @Schema(description = "图标")
    private String icon;

    /**
     * 排序
     */
    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;

    /**
     * 权限标识
     */
    @Schema(description = "权限标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonAlias({"permission", "authCode"})
    private String permission = "";

    @Schema(description = "路由组件")
    private String component;

    @Schema(description = "元数据")
    private Map<String, Object> meta;

    @Schema(description = "状态")
    public void setStatus(Integer status) {
        if (status == null) return;
        this.valid = status == 1;
    }
}
