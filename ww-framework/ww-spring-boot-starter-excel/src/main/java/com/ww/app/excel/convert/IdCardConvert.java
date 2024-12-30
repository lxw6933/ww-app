package com.ww.app.excel.convert;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.ww.app.common.enums.SensitiveDataType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ww
 * @create 2024-12-22 11:25
 * @description: 身份证号写入excel转换器
 */
@Slf4j
public class IdCardConvert implements Converter<String> {

    @Override
    public WriteCellData<String> convertToExcelData(String value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (StringUtils.isBlank(value)) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(SensitiveDataType.ID_CARD.getDesensitizer().apply(value));
    }

}
