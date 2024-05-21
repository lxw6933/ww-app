package com.ww.mall.admin.view.query;

import com.ww.mall.web.cmmon.MallPage;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2024-05-21- 14:47
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserPageQuery extends MallPage {

    private String username;

    private Boolean status;

    private Boolean valid;

}
