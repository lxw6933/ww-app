package com.ww.mall.web.excel;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
public class ExcelManager {

    /**
     * 下载excel（导出所有属性字段在一个sheet里）
     *
     * @param response  web响应
     * @param data      导出数据集合
     * @param pojoClass 数据类型
     * @param fileName  导出excel文件名称
     * @param sheetName sheet名称
     */
    public <T, E> void exportExcelOfOneSheet(HttpServletResponse response, List<T> data, Class<E> pojoClass, String fileName, String sheetName) throws IOException {
        exportExcelOfOneSheet(response, data, pojoClass, fileName, sheetName, null, null);
    }

    /**
     * 下载excel 在一个sheet里
     *
     * @param response                web响应
     * @param data                    导出数据集合
     * @param pojoClass               数据类型
     * @param fileName                导出excel文件名称
     * @param sheetName               sheet名称
     * @param excludeColumnFiledNames 不需要显示的字段名称集合
     * @param includeColumnFiledNames 需要显示的字段名称集合
     */
    public <T, E> void exportExcelOfOneSheet(HttpServletResponse response, List<T> data, Class<E> pojoClass, String fileName, String sheetName, Set<String> excludeColumnFiledNames, Set<String> includeColumnFiledNames) throws IOException {
        try {
            setResponse(response, fileName);
            if (CollectionUtils.isNotEmpty(includeColumnFiledNames)) {
                EasyExcelFactory.write(response.getOutputStream(), pojoClass)
                        .includeColumnFiledNames(includeColumnFiledNames)
                        .sheet(sheetName)
                        .doWrite(data);
            } else if (CollectionUtils.isNotEmpty(excludeColumnFiledNames)) {
                EasyExcelFactory.write(response.getOutputStream(), pojoClass)
                        .excludeColumnFiledNames(excludeColumnFiledNames)
                        .sheet(sheetName)
                        .doWrite(data);
            } else {
                EasyExcelFactory.write(response.getOutputStream(), pojoClass)
                        .sheet(sheetName)
                        .doWrite(data);
            }
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
        ExcelWriter excelWriter = null;
        try {
            setResponse(response, fileName);
            // 指定excel文件
            excelWriter = EasyExcelFactory.write(response.getOutputStream()).build();
            int i = 0;
            for (Map.Entry<String, List<T>> entry : data.entrySet()) {
                // 每次都要创建 writeSheet 这里注意必须指定sheetNo 而且sheetName必须不一样。
                WriteSheet writeSheet = EasyExcelFactory.writerSheet(i, entry.getKey()).head(entry.getValue().get(0).getClass()).build();
                excelWriter.write(entry.getValue(), writeSheet);
                i++;
            }
        } catch (Exception e) {
            exportErrorReturn(response, e);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
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

    private void setResponse(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("UTF-8");
        // 这里URLEncoder.encode可以防止中文乱码
        fileName = URLEncoder.encode(fileName, "UTF-8")
                .replace("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
    }

    private void exportErrorReturn(HttpServletResponse response, Exception e) throws IOException {
        log.error("导出excel失败！！！导出失败原因：[{}]", e.getMessage());
        // 重置response
        response.reset();
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        Map<String, String> map = new HashMap<>(64);
        map.put("status", "failure");
        map.put("message", "下载文件失败" + e.getMessage());
        response.getWriter().println(JSON.toJSONString(map));
    }

}
