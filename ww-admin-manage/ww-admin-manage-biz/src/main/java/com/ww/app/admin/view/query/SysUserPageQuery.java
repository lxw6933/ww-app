package com.ww.app.admin.view.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.app.admin.entity.SysUser;
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
@Schema(description = "用户列表查询")
public class SysUserPageQuery extends AppPage {

    @Schema(description = "用户名称")
    private String username;

    @Schema(description = "用户状态")
    private Boolean status;

    @Schema(description = "是否有效用户")
    private Boolean valid;

    public QueryWrapper<SysUser> getQueryWrapper() {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(this.username)) {
            queryWrapper.like("username", this.username);
        }
        if (this.valid != null) {
            queryWrapper.eq("valid", this.valid);
        }
        if (this.status != null) {
            queryWrapper.eq("status", this.status);
        }
        return queryWrapper;
    }

}
