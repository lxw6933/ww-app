package com.ww.app.admin.controller.area;

import cn.hutool.core.lang.Assert;
import com.ww.app.admin.controller.area.res.AreaNodeVO;
import com.ww.app.admin.convert.area.AreaConvert;
import com.ww.app.ip.common.Area;
import com.ww.app.ip.utils.AreaUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-25 18:24
 * @description:
 */
@RestController
@RequestMapping("/system/area")
@Tag(name = "管理后台 - 地区 API")
public class AreaController {

    @GetMapping("/tree")
    @Operation(summary = "获得地区树")
    public List<AreaNodeVO> getAreaTree() {
        Area area = AreaUtils.getArea(Area.ID_CHINA);
        Assert.notNull(area, "获取不到中国地址数据");
        return AreaConvert.INSTANCE.convertList(area.getChildren());
    }

}
