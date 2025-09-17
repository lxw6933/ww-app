package com.ww.app.excel;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CommonUtils;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.minio.MinioTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

/**
 * @author ww
 * @create 2025-09-17 10:34
 * @description:
 */
@Slf4j
@Component
public class ExcelMinioTemplate {

    @Resource
    private MinioTemplate minioTemplate;

    @Resource
    private ExcelTemplate excelTemplate;

    private final ExecutorService defaultExportExecutor = ThreadUtil.initFixedThreadPoolExecutor("ww-excel-export", 20);

    public <T> String exportDataToMinio(String bucketName, int totalCount, int fileDataSize, BiFunction<Integer, Integer, List<T>> pageFunction) {
        return exportDataToMinio(bucketName, totalCount, fileDataSize, pageFunction, defaultExportExecutor);
    }

    public <T> String exportDataToMinio(String bucketName, int totalCount, int fileDataSize, BiFunction<Integer, Integer, List<T>> pageFunction, ExecutorService exportExecutor) {
        int sheetNumber = CommonUtils.getCircleNumber(totalCount, fileDataSize);
        // 每个文件都开启一个线程去写入
        CountDownLatch countDownLatch = new CountDownLatch(sheetNumber);

        List<File> exportFiles = new CopyOnWriteArrayList<>();
        File targetFile = null;
        try {
            for (int i = 0; i < sheetNumber; i++) {
                final int sheetIndex = i;
                CompletableFuture.runAsync(() -> {
                    List<T> dataList = pageFunction.apply(sheetIndex, sheetNumber);
                    // 生成临时文件
                    File file = excelTemplate.exportExcelOfOneSheetToTempFile(dataList, sheetIndex + StrUtil.EMPTY, UUID.randomUUID() + StrUtil.UNDERLINE + sheetIndex);
                    exportFiles.add(file);
                }, exportExecutor).exceptionally(e -> {
                    throw new RuntimeException("导出临时文件异常", e);
                }).thenRun(countDownLatch::countDown);
            }
            countDownLatch.await();
            targetFile = ZipUtil.zip(FileUtil.createTempFile(UUID.randomUUID().toString(), ".zip", true), true, exportFiles.toArray(new File[]{}));
            try (FileInputStream inputStream = new FileInputStream(targetFile)) {
                boolean upload = minioTemplate.upload(inputStream, bucketName, targetFile.getName());
                log.info("导出压缩文件上传minio结果[{}]", upload);
                if (!upload) {
                    throw new ApiException("上传压缩文件失败");
                }
            }
            log.info("上传压缩文件至Minio完成");
            return minioTemplate.getFileUrl(bucketName, targetFile.getName(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (targetFile != null) {
                boolean del = FileUtil.del(targetFile);
                log.info("导出临时压缩文件删除结果[{}]", del);
            }
            if (!exportFiles.isEmpty()) {
                exportFiles.forEach(res -> {
                    boolean del = FileUtil.del(res);
                    log.info("导出临时文件删除结果[{}]", del);
                });
            }
        }
    }

}
