package com.ww.mall.excel.convert;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.ww.mall.common.enums.SensitiveDataType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ww
 * @create 2024-12-22 11:25
 * @description: 银行卡号写入excel转换器
 */
@Slf4j
public class BankNoConvert implements Converter<String> {

    @Override
    public WriteCellData<String> convertToExcelData(String value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (StringUtils.isBlank(value)) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(SensitiveDataType.BANK_CARD.getDesensitizer().apply(value));
    }

}
