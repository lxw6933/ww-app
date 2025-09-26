package com.ww.app.admin.convert.area;

import com.ww.app.admin.controller.area.res.AreaNodeVO;
import com.ww.app.ip.common.Area;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-25 20:07
 * @description:
 */
@Mapper
public interface AreaConvert {

    AreaConvert INSTANCE = Mappers.getMapper(AreaConvert.class);

    List<AreaNodeVO> convertList(List<Area> areaList);

}
