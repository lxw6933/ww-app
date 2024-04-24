package com.ww.mall.minio.s3;

import cn.hutool.core.text.CharSequenceUtil;
import com.google.common.collect.HashMultimap;
import com.ww.mall.common.exception.ApiException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListPartsResponse;
import io.minio.http.Method;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2024-04-24- 10:11
 * @description:
 */
@Slf4j
@Component
public class MallMinioUtil {

    private final static Integer MAX_CHUNK_NUMBER = 1000;

    @Resource
    private MallMinioClient mallMinioClient;

    public Map<String, Object> chunkFileUpload(String uploadId,
                                               String md5,
                                               String chunkFileName,
                                               String contentType,
                                               int chunkNumber) {
        log.info("【开始分片上传：{}】bucket:【{}】chunkFileName:【{}】chunkNumber:【{}】contentType:【{}】", uploadId, md5, chunkFileName, chunkNumber, contentType);
        Map<String, Object> resMap = new HashMap<>();
        if (CharSequenceUtil.isBlank(contentType)) {
            // 使用默认流会导致无法预览
            contentType = "application/octet-stream";
        }
        HashMultimap<String, String> headers = HashMultimap.create();
        headers.put("Content-Type", contentType);
        // 获取分片上传的uploadId
        if (StringUtils.isEmpty(uploadId)) {
            try {
                uploadId = mallMinioClient.chunkFileUpload(md5, null, chunkFileName, headers, null);
            } catch (Exception e) {
                log.error("【初始化】分片上传异常:{}", e.getMessage());
                throw new ApiException("【初始化】分片上传异常");
            }
        }
        resMap.put("uploadId", uploadId);
        resMap.put("chunkNumber", chunkNumber);
        // 收集所有分片文件
        List<String> partList = new ArrayList<>();

        Map<String, String> chunkFileParams = new HashMap<>();
        chunkFileParams.put("uploadId", uploadId);
        try {
            for (int i = 1; i <= chunkNumber; i++) {
                chunkFileParams.put("partNumber", String.valueOf(i));
                String uploadUrl = mallMinioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(md5)
                                .object(chunkFileName)
                                .expiry(1, TimeUnit.DAYS)
                                .extraQueryParams(chunkFileParams)
                                .build());
                partList.add(uploadUrl);
            }
        } catch (Exception e) {
            log.error("分片上传异常:{}", e.getMessage());
            throw new ApiException("分片上传异常");
        }
        log.info("bucket【{}】uploadId【{}】分片上传成功", md5, uploadId);
        resMap.put("urlList", partList);
        return resMap;
    }

    public boolean mergeChunkFile(String chunkBucketName,
                                  String chunkFileName,
                                  String uploadId) {
        log.info("开始合并分片文件uploadId:【{}】bucket:【{}】filename:【{}】", uploadId, chunkBucketName, chunkFileName);
        Part[] parts = new Part[MAX_CHUNK_NUMBER];
        try {
            // 查询uploadId上传的所有分片文件
            ListPartsResponse partResult = mallMinioClient.listMultipart(chunkBucketName, null, chunkFileName, MAX_CHUNK_NUMBER, 0, uploadId, null, null);
            int partNumber = 1;
            for (Part part : partResult.result().partList()) {
                parts[partNumber - 1] = new Part(partNumber, part.etag());
                partNumber++;
            }
            // 合并分片文件
            mallMinioClient.mergeChunkFile(chunkBucketName, null, chunkFileName, uploadId, parts, null, null);
        } catch (Exception e) {
            log.error("分片文件【{}】合并异常:{}", uploadId, e.getMessage());
            throw new ApiException("分片文件合并异常");
        }
        return true;
    }


    public List<Integer> getChunkFile(String md5, String chunkFileName, String uploadId) {
        log.info("查询上传分片数据{} -> {} -> {}", md5, chunkFileName, uploadId);
        try {
            // 查询上传后的分片数据
            ListPartsResponse partResult = mallMinioClient.listMultipart(md5, null, chunkFileName, MAX_CHUNK_NUMBER, 0, uploadId, null, null);
            return partResult.result().partList().stream().map(Part::partNumber).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取分片数据异常: {}", e.getMessage());
            throw new ApiException("获取分片数据异常");
        }
    }




}
