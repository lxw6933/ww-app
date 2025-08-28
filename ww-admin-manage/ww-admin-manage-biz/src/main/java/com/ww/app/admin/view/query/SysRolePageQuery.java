package com.ww.app.admin.view.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.app.admin.entity.SysRole;
import com.ww.app.common.common.AppPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ww
 * @create 2024-05-21- 14:47
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色列表查询")
public class SysRolePageQuery extends AppPage {

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色状态")
    private Boolean status;

    public QueryWrapper<SysRole> getQueryWrapper() {
        QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(this.name)) {
            queryWrapper.like("name", this.name);
        }
        if (status != null) {
            queryWrapper.eq("status", this.status);
        }
        return queryWrapper;
    }

}
