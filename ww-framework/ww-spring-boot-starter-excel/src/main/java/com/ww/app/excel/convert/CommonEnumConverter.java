package com.ww.app.excel.convert;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.ww.app.common.enums.BaseEnum;

import java.lang.reflect.Method;

/**
 * @author ww
 * @create 2025-03-29- 15:48
 * @description:
 */
public class CommonEnumConverter implements Converter<BaseEnum> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return BaseEnum.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public BaseEnum convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        Class<?> fieldType = contentProperty.getField().getType();
        if (!BaseEnum.class.isAssignableFrom(fieldType)) {
            throw new IllegalArgumentException("Field type must implement BaseEnum interface");
        }

        try {
            Method fromValueMethod = fieldType.getMethod("fromShowValue", Class.class, Object.class);
            return (BaseEnum) fromValueMethod.invoke(null, fieldType, cellData.getStringValue());
        } catch (Exception e) {
            throw new RuntimeException("Convert enum failed: " + e.getMessage(), e);
        }
    }

    @Override
    public WriteCellData<?> convertToExcelData(BaseEnum value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        return new WriteCellData<>(value.getShowValue());
    }
}
