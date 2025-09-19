package com.ww.app.operatelog.core.convert;

import com.ww.app.operatelog.core.entity.OperateLog;
import com.ww.app.operatelog.view.dto.OperateLogDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-09-19 15:10
 * @description:
 */
@Mapper
public interface OperateLogConvert {

    OperateLogConvert INSTANCE = Mappers.getMapper(OperateLogConvert.class);

    OperateLog convert(OperateLogDTO operateLogDTO);

}
