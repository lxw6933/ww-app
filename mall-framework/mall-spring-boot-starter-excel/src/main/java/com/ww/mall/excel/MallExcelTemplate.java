package com.ww.mall.excel;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description: excel管理类
 * @author: ww
 * @create: 2021-05-14 14:04
 */
@Slf4j
@Component
public class MallExcelTemplate {

    /**
     * 下载excel（导出所有属性字段在一个sheet里）
     *
     * @param response  web响应
     * @param data      导出数据集合
     * @param pojoClass 数据类型
     * @param fileName  导出excel文件名称
     * @param sheetName sheet名称
     */
    public <T> void exportExcelOfOneSheet(HttpServletResponse response, List<T> data, Class<T> pojoClass, String fileName, String sheetName) throws IOException {
        exportExcelOfOneSheet(response, data, pojoClass, fileName, sheetName, null, true);
    }

    /**
     * 下载excel 在一个sheet里
     *
     * @param response   web响应
     * @param data       导出数据集合
     * @param pojoClass  数据类型
     * @param fileName   导出excel文件名称
     * @param sheetName  sheet名称
     * @param filedNames 字段名称集合
     * @param include    true: 只显示filedNames false：不包含filedNames
     */
    public <T> void exportExcelOfOneSheet(HttpServletResponse response, List<T> data, Class<T> pojoClass, String fileName, String sheetName, Set<String> filedNames, boolean include) throws IOException {
        try {
            setResponse(response, fileName);
            ExcelWriterBuilder excelWriterBuilder = EasyExcelFactory.write(response.getOutputStream(), pojoClass);
            if (CollectionUtils.isNotEmpty(filedNames)) {
                if (include) {
                    excelWriterBuilder.includeColumnFieldNames(filedNames);
                } else {
                    excelWriterBuilder.excludeColumnFieldNames(filedNames);
                }
            }
            excelWriterBuilder.sheet(sheetName).doWrite(data);
        } catch (Exception e) {
            exportErrorReturn(response, e);
        }
    }

    /**
     * 下载excel 多个集合分布在不同的sheet里
     *
     * @param response web响应
     * @param data     导出数据集合 string: sheet名称   data：对应的数据集合
     * @param fileName 导出excel文件名称
     */
    public <T> void exportExcelOfManySheet(HttpServletResponse response, Map<String, List<T>> data, String fileName) throws IOException {
        exportExcelOfManySheet(response, data, fileName, null, true);
    }

    public <T> void exportExcelOfManySheet(HttpServletResponse response, Map<String, List<T>> data, String fileName, Set<String> filedNames, boolean include) throws IOException {
        setResponse(response, fileName);
        try (ExcelWriter excelWriter = EasyExcelFactory.write(response.getOutputStream()).build()) {
            int i = 0;
            for (Map.Entry<String, List<T>> entry : data.entrySet()) {
                // 每次都要创建 writeSheet 这里注意必须指定sheetNo 而且sheetName必须不一样。
                ExcelWriterSheetBuilder excelWriterSheetBuilder = EasyExcelFactory.writerSheet(i, entry.getKey()).head(entry.getValue().get(0).getClass());
                if (CollectionUtils.isNotEmpty(filedNames)) {
                    if (include) {
                        excelWriterSheetBuilder.includeColumnFieldNames(filedNames);
                    } else {
                        excelWriterSheetBuilder.excludeColumnFieldNames(filedNames);
                    }
                }
                WriteSheet writeSheet = excelWriterSheetBuilder.build();
                excelWriter.write(entry.getValue(), writeSheet);
                i++;
            }
        } catch (Exception e) {
            exportErrorReturn(response, e);
        }
    }

    /**
     * 读取上传的excel文件
     *
     * @param file       file
     * @param modelClass 封装数据model类型
     */
    public <T> void readExcel(MultipartFile file, Class<T> modelClass, MallAbstractImportListener<T> listener) throws IOException {
        EasyExcelFactory.read(
                file.getInputStream(),
                modelClass,
                listener
        ).sheet().doRead();
    }

    /**
     * 读取上传的excel文件的指定sheet数据
     *
     * @param file       file
     * @param sheetNum   sheet下标
     * @param modelClass 封装数据model类型
     */
    public <T> void readExcel(MultipartFile file, int sheetNum, Class<T> modelClass, MallAbstractImportListener<T> listener) throws IOException {
        EasyExcelFactory.read(
                file.getInputStream(),
                modelClass,
                listener
        ).sheet(sheetNum).doRead();
    }

    private void setResponse(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 这里URLEncoder.encode可以防止中文乱码
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replace("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
    }

    private void exportErrorReturn(HttpServletResponse response, Exception e) throws IOException {
        log.error("导出excel失败！！！导出失败原因：", e);
        // 重置response
        response.reset();
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, String> map = new HashMap<>(64);
        map.put("status", "failure");
        map.put("message", "下载文件失败" + e.getMessage());
        response.getWriter().println(JSON.toJSONString(map));
    }

}
