package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.SysUserEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 后台用户表 - Query
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysUserQuery {

    /**
     * 中心端ID
     */
    private Long centerId;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 账号状态（1：正常；0：冻结）
     */
    private Integer status;


    public QueryWrapper<SysUserEntity> getQueryWrapper() {
        QueryWrapper<SysUserEntity> wrapper = new QueryWrapper<>();
        if (this.centerId != null) {
            wrapper.eq("`center_id`", this.centerId);
        }
        if (StringUtils.isNotBlank(this.username)) {
            wrapper.like("`username`", this.username);
        }
        if (StringUtils.isNotBlank(this.nickname)) {
            wrapper.like("`nickname`", this.nickname);
        }
        if (this.status != null) {
            wrapper.eq("`status`", this.status);
        }
        return wrapper;
    }

}