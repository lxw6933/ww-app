package com.ww.app.minio;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.HashMultimap;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CollectionUtils;
import com.ww.app.minio.bo.ChunkFileMergeReqBO;
import com.ww.app.minio.bo.ChunkFileUploadReqBO;
import com.ww.app.minio.vo.CreateMultipartUploadResultVO;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MinioTemplate {

    private final static Integer MAX_CHUNK_NUMBER = 1000;

    private final MinioClient minioClient;

    private final MinioS3Client minioS3Client;

    public MinioTemplate(MinioClient minioClient, MinioS3Client minioS3Client) {
        this.minioClient = minioClient;
        this.minioS3Client = minioS3Client;
    }

    /**
     * 创建bucket
     *
     * @param bucketName bucketName
     * @return boolean
     */
    public boolean createBucket(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
            }
            return true;
        } catch (Exception e) {
            log.error("create bucket exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 是否存在bucket
     *
     * @param bucketName bucketName
     * @return boolean
     */
    public boolean existBucket(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("exist bucket exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 查询桶内所有对象
     *
     * @param bucketName bucketName
     * @param recursive  是否递归查询
     * @return list
     */
    public List<Item> listBucketAllFile(String bucketName, boolean recursive) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(recursive)
                    .build());
            Iterator<Result<Item>> iterator = results.iterator();
            List<Item> items = new ArrayList<>();
            while (iterator.hasNext()) {
                items.add(iterator.next().get());
            }
            return items;
        } catch (Exception e) {
            log.error("查询桶内所有文件异常：", e);
            return null;
        }
    }

    /**
     * 删除bucket
     *
     * @param bucketName bucketName
     * @return boolean
     */
    public Boolean removeBucket(String bucketName) {
        try {
            // 删除桶内文件
            List<Item> bucketFiles = listBucketAllFile(bucketName, true);
            List<String> fileNames = CollectionUtils.convertList(bucketFiles, Item::objectName);
            if (CollectionUtil.isNotEmpty(fileNames)) {
                fileNames.forEach(fileName -> removeFile(bucketName, fileName));
            }
            // 先删除文件，再删除bucket
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("bucket remove exception: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 根据filename获取文件访问地址
     *
     * @param objectName     文件名称
     * @param expiryTime     过期时间
     * @param expiryTimeUnit 过期时间单位
     * @param queryParams    查询参数
     * @return url
     */
    public String getExpiryFileUrl(String bucketName, String objectName, Map<String, String> queryParams, int expiryTime, TimeUnit expiryTimeUnit) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryTime, expiryTimeUnit)
                            .extraQueryParams(queryParams)
                            .build());
        } catch (Exception e) {
            log.error("获取文件expiry url异常：", e);
            return null;
        }
    }

    public String getFileUrl(String bucketName, String objectName, Map<String, String> queryParams) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .extraQueryParams(queryParams)
                            .build());
        } catch (Exception e) {
            log.error("获取文件url异常：", e);
            return null;
        }
    }

    /**
     * 上传文件
     *
     * @param file       文件 [5MiB to 5GiB]
     * @param bucketName bucketName
     * @param fileName   文件名称
     */
    public boolean upload(MultipartFile file, String bucketName, String fileName) {
        createBucket(bucketName);
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return true;
        } catch (Exception e) {
            log.error("上传文件异常：", e);
            return false;
        }
    }

    public boolean upload(byte[] fileBytes, String bucketName, String fileName) {
        createBucket(bucketName);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(byteArrayInputStream, fileBytes.length, -1)
                    .contentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM).toString())
                    .build());
            return true;
        } catch (Exception e) {
            log.error("上传文件异常：", e);
            return false;
        }
    }

    public boolean upload(InputStream fileInputStream, String bucketName, String fileName) {
        createBucket(bucketName);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(fileInputStream, fileInputStream.available(), -1)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("上传文件异常：", e);
            return false;
        }
    }

    /**
     * 文件合并，将分块文件组成一个新的文件
     *
     * @param originBucketName 分块文件所在的桶
     * @param targetBucketName 合并文件生成文件所在的桶
     * @param fileName         文件名称
     * @return boolean
     */
    public boolean mergeFile(String originBucketName, String targetBucketName, String fileName) {
        // 获取桶内所有文件
        List<Item> bucketFiles = listBucketAllFile(targetBucketName, true);
        List<String> fileNameList = CollectionUtils.convertList(bucketFiles, Item::objectName);
        List<ComposeSource> composeSourceList = new ArrayList<>(fileNameList.size());
        // 对文件名集合进行升序排序
        Collections.sort(fileNameList);
        for (String object : fileNameList) {
            composeSourceList.add(ComposeSource.builder()
                    .bucket(originBucketName)
                    .object(object)
                    .build());
        }
        // 合并桶内所有文件
        try {
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(targetBucketName)
                    .object(fileName)
                    .sources(composeSourceList)
                    .build());
        } catch (Exception e) {
            log.error("merge file exception: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 删除文件
     *
     * @param bucketName bucketName
     * @param fileName   文件名称
     * @return boolean
     */
    public boolean removeFile(String bucketName, String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("文件删除异常：", e);
            return false;
        }
    }

    /**
     * 下载文件
     *
     * @param bucketName bucketName
     * @param fileName   文件名称
     * @return response
     */
    public ResponseEntity<byte[]> download(String bucketName, String fileName) {
        ResponseEntity<byte[]> responseEntity = null;
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(in, out);
            //封装返回值
            byte[] bytes = out.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.displayName()));
            headers.setContentLength(bytes.length);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setAccessControlExposeHeaders(Collections.singletonList("*"));
            responseEntity = new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("下载文件异常：", e);
        }
        return responseEntity;
    }

    public String generateFileInMinioName(String originalFilename) {
        return "files" + StrUtil.SLASH + DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN) + StrUtil.SLASH + UUID.randomUUID() + StrUtil.UNDERLINE + originalFilename;
    }

    /**
     * 查询文件大小
     *
     * @param bucketName bucketName
     * @param fileName   文件名称
     * @return fileSize
     */
    public Long getFileSize(String bucketName, String fileName) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build())
                    .size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取文件大小【格式化后】
     *
     * @param fileSize 文件大小
     * @return size
     */
    private String formatFileSize(long fileSize) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeStr;
        if (fileSize == 0) {
            return "0B";
        }
        if (fileSize < 1024) {
            fileSizeStr = df.format((double) fileSize) + " B";
        } else if (fileSize < (1024 * 1024)) {
            fileSizeStr = df.format((double) fileSize / 1024) + " KB";
        } else if (fileSize < (1024 * 1024 * 1024)) {
            fileSizeStr = df.format((double) fileSize / (1024 * 1024)) + " MB";
        } else {
            fileSizeStr = df.format((double) fileSize / (1024 * 1024 * 1024)) + " GB";
        }
        return fileSizeStr;
    }

    /**
     * 分片上传文件
     *
     * @param reqBO bo
     * @return vo
     */
    public CreateMultipartUploadResultVO chunkFileUpload(ChunkFileUploadReqBO reqBO) {
        log.info("开始分片上传：[{}]", JSON.toJSONString(reqBO));
        // 1. 收集分片信息
        Map<String, Object> resMap = new HashMap<>();
        if (CharSequenceUtil.isEmpty(reqBO.getContentType())) {
            // 使用默认流会导致无法预览
            reqBO.setContentType("application/octet-stream");
        }
        HashMultimap<String, String> headers = HashMultimap.create();
        headers.put("Content-Type", reqBO.getContentType());
        // 2. 获取分片上传的uploadId
        if (StringUtils.isEmpty(reqBO.getUploadId())) {
            try {
                // 初始化分片上传，获取uploadId
                String uploadId = minioS3Client.createMultipartUpload(reqBO.getFileMd5(), null, reqBO.getFileName(), headers, null).result().uploadId();
                reqBO.setUploadId(uploadId);
            } catch (Exception e) {
                log.error("【初始化】分片上传异常:{}", e.getMessage());
                throw new ApiException("【初始化】分片上传异常");
            }
        }
        // 3. 请求Minio 服务，获取每个分块带签名的上传URL
        Map<String, String> chunkFileParams = new HashMap<>();
        chunkFileParams.put("uploadId", reqBO.getUploadId());
        // 4. 循环分块数 从1开始,MinIO 存储服务定义分片索引却是从1开始的
        for (int i = 1; i <= reqBO.getChunkSize(); i++) {
            chunkFileParams.put("partNumber", String.valueOf(i));
            // 获取URL,主要这里前端上传的时候，要传递二进制流，而不是file
            String uploadUrl = this.getFileUrl(reqBO.getFileMd5(), reqBO.getFileName(), chunkFileParams);
            resMap.put("chunk_" + (i - 1), uploadUrl);
        }
        log.info("bucket[{}]uploadId[{}]分片上传成功", reqBO.getFileMd5(), reqBO.getUploadId());
        CreateMultipartUploadResultVO createMultipartUploadResultVO = new CreateMultipartUploadResultVO();
        createMultipartUploadResultVO.setUploadId(reqBO.getUploadId());
        createMultipartUploadResultVO.setChunkFileList(resMap);
        return createMultipartUploadResultVO;
    }

    /**
     * 合并分片文件
     *
     * @param reqBO bo
     * @return boolean
     */
    public boolean chunkFileMerge(ChunkFileMergeReqBO reqBO) {
        log.info("开始合并分片文件:【{}】", reqBO);
        // 获取所有分片文件
        ListPartsResult partsResult;
        try {
            partsResult = minioS3Client.listMultipart(reqBO.getFileMd5(), null, reqBO.getFileName(), MAX_CHUNK_NUMBER, 0, reqBO.getUploadId(), null, null).result();
        } catch (Exception e) {
            log.error("获取存储桶内分片文件异常:{}", e.getMessage());
            throw new ApiException("获取存储桶内分片文件异常");
        }
        if (partsResult.partList().size() > MAX_CHUNK_NUMBER) {
            throw new ApiException("超出最大分片数量合并");
        }
        // 收集分片集合
        Part[] parts = new Part[partsResult.partList().size()];
        int partNumber = 1;
        for (Part part : partsResult.partList()) {
            parts[partNumber - 1] = new Part(partNumber, part.etag());
            partNumber++;
        }
        // 分片合并
        try {
            minioS3Client.mergeMultipartUploadFile(reqBO.getFileMd5(), null, reqBO.getFileName(), reqBO.getUploadId(), parts, null, null);
        } catch (Exception e) {
            log.error("合并分片文件异常:{}", e.getMessage());
            throw new ApiException("合并分片文件异常");
        }
        return true;
    }



}
