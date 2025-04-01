package com.ww.app.excel;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.converters.longconverter.LongStringConverter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: ww
 * @create: 2021-05-14 14:04
 * @description: Excel工具类
 */
@Slf4j
@Component
public class ExcelTemplate {

    /**
     * 默认文件后缀
     */
    private static final String DEFAULT_SUFFIX = ".xlsx";

    /**
     * 默认sheet名称
     */
    private static final String DEFAULT_SHEET_NAME = "Sheet1";

    /**
     * 默认缓冲区大小
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 创建基础Excel写入构建器
     *
     * @param os 输出流
     * @param pojoClass 数据类型
     * @return ExcelWriterBuilder
     */
    private <T> ExcelWriterBuilder baseBuilder(OutputStream os, Class<T> pojoClass) {
        return EasyExcel.write(os, pojoClass)
                // 不要自动关闭流
                .autoCloseStream(false)
                // 基于column长度，自动适配。最大 255 宽度
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                // 避免Long类型丢失精度
                .registerConverter(new LongStringConverter());
    }

    /**
     * 导出Excel到临时文件(单sheet)
     *
     * @param data 数据列表
     * @return 临时文件
     */
    public <T> File exportExcelOfOneSheetToTempFile(List<T> data) {
        return exportExcelOfOneSheetToTempFile(data, DEFAULT_SHEET_NAME, DEFAULT_SUFFIX);
    }

    /**
     * 导出Excel到临时文件(单sheet)
     *
     * @param data 数据列表
     * @param sheetName sheet名称
     * @param fileSuffix 文件后缀
     * @return 临时文件
     */
    public <T> File exportExcelOfOneSheetToTempFile(List<T> data, String sheetName, String fileSuffix) {
        if (CollectionUtils.isEmpty(data)) {
            throw new ApiException("导出数据不能为空");
        }

        File tempFile;
        try {
            // 创建临时文件路径
            tempFile = FileUtil.createTempFile(UUID.randomUUID().toString(true), fileSuffix + DEFAULT_SUFFIX, true);
            // 导出数据到临时文件
            try (OutputStream os = Files.newOutputStream(tempFile.toPath())) {
                baseBuilder(os, data.get(0).getClass())
                        .sheet(sheetName)
                        .doWrite(data);
            }
            return tempFile;
        } catch (IOException e) {
            log.error("生成临时文件失败: ", e);
            throw new ApiException("导出文件失败: " + e.getMessage());
        }
    }

    /**
     * 导出Excel(单sheet)
     *
     * @param response HTTP响应
     * @param data 数据列表
     * @param fileName 文件名
     * @param sheetName sheet名称
     */
    public <T> void exportExcelOfOneSheet(HttpServletResponse response, List<T> data, String fileName, String sheetName) throws IOException {
        exportExcelOfOneSheet(response, data, fileName, sheetName, null, true);
    }

    /**
     * 导出Excel(单sheet)
     *
     * @param response HTTP响应
     * @param data 数据列表
     * @param fileName 文件名
     * @param sheetName sheet名称
     * @param fieldNames 字段名称集合
     * @param include true:只显示fieldNames false:不包含fieldNames
     */
    public <T> void exportExcelOfOneSheet(HttpServletResponse response, List<T> data, String fileName, 
            String sheetName, Set<String> fieldNames, boolean include) throws IOException {
        if (CollectionUtils.isEmpty(data)) {
            throw new ApiException("导出数据不能为空");
        }

        try {
            setResponse(response, fileName);
            ExcelWriterBuilder builder = baseBuilder(response.getOutputStream(), data.get(0).getClass());
            
            if (CollectionUtils.isNotEmpty(fieldNames)) {
                if (include) {
                    builder.includeColumnFieldNames(fieldNames);
                } else {
                    builder.excludeColumnFieldNames(fieldNames);
                }
            }
            
            builder.sheet(sheetName).doWrite(data);
        } catch (Exception e) {
            exportErrorReturn(response, e);
        }
    }

    /**
     * 导出Excel(多sheet)
     *
     * @param response HTTP响应
     * @param data 数据Map
     * @param fileName 文件名
     */
    public <T> void exportExcelOfManySheet(HttpServletResponse response, Map<String, List<T>> data, String fileName) throws IOException {
        exportExcelOfManySheet(response, data, fileName, null, true);
    }

    /**
     * 导出Excel(多sheet)
     *
     * @param response HTTP响应
     * @param data 数据Map
     * @param fileName 文件名
     * @param fieldNames 字段名称集合
     * @param include true:只显示fieldNames false:不包含fieldNames
     */
    public <T> void exportExcelOfManySheet(HttpServletResponse response, Map<String, List<T>> data, 
            String fileName, Set<String> fieldNames, boolean include) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new ApiException("导出数据不能为空");
        }

        setResponse(response, fileName);
        try (ExcelWriter excelWriter = baseBuilder(response.getOutputStream(), 
                data.values().iterator().next().get(0).getClass()).build()) {
            
            int sheetNo = 0;
            for (Map.Entry<String, List<T>> entry : data.entrySet()) {
                ExcelWriterSheetBuilder sheetBuilder = EasyExcelFactory.writerSheet(sheetNo, entry.getKey())
                        .head(entry.getValue().get(0).getClass());
                
                if (CollectionUtils.isNotEmpty(fieldNames)) {
                    if (include) {
                        sheetBuilder.includeColumnFieldNames(fieldNames);
                    } else {
                        sheetBuilder.excludeColumnFieldNames(fieldNames);
                    }
                }
                
                WriteSheet writeSheet = sheetBuilder.build();
                excelWriter.write(entry.getValue(), writeSheet);
                sheetNo++;
            }
        } catch (Exception e) {
            exportErrorReturn(response, e);
        }
    }

    /**
     * 读取Excel文件
     *
     * @param file 文件
     * @param modelClass 数据类型
     * @param listener 监听器
     */
    public <T> void readExcel(MultipartFile file, Class<T> modelClass, AbstractImportListener<T> listener) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException("上传文件不能为空");
        }

        try {
            EasyExcelFactory.read(file.getInputStream(), modelClass, listener)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            log.error("读取Excel文件失败: ", e);
            throw new ApiException("读取Excel文件失败: " + e.getMessage());
        }
    }

    /**
     * 读取Excel文件指定sheet
     *
     * @param file 文件
     * @param sheetNum sheet下标
     * @param modelClass 数据类型
     * @param listener 监听器
     */
    public <T> void readExcel(MultipartFile file, int sheetNum, Class<T> modelClass, 
            AbstractImportListener<T> listener) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException("上传文件不能为空");
        }

        try {
            EasyExcelFactory.read(file.getInputStream(), modelClass, listener)
                    .sheet(sheetNum)
                    .doRead();
        } catch (Exception e) {
            log.error("读取Excel文件失败: ", e);
            throw new ApiException("读取Excel文件失败: " + e.getMessage());
        }
    }

    /**
     * 设置HTTP响应头
     */
    private void setResponse(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replace("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + DEFAULT_SUFFIX);
    }

    /**
     * 导出错误响应
     */
    private void exportErrorReturn(HttpServletResponse response, Exception e) throws IOException {
        log.error("导出Excel失败: ", e);
        response.reset();
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        Map<String, String> result = new HashMap<>(2);
        result.put("status", "failure");
        result.put("message", "下载文件失败: " + e.getMessage());
        
        response.getWriter().println(JSON.toJSONString(result));
    }
}
