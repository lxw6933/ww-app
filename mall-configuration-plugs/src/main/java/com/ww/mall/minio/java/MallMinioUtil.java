package com.ww.mall.minio.java;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.mall.common.exception.ApiException;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnBean(MinioClient.class)
public class MallMinioUtil {

    @Resource
    private MinioClient minioClient;

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
     * @param recursive 是否递归查询
     * @return iterable
     */
    public Iterable<Result<Item>> listBucketAllFile(String bucketName, boolean recursive) {
        try {
            return minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(recursive)
                    .build());
        } catch (Exception e) {
            log.error("查询桶内所有文件异常：{}", e.getMessage());
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
            Iterable<Result<Item>> bucketFiles = listBucketAllFile(bucketName, true);
            Iterator<Result<Item>> iterator = bucketFiles.iterator();
            List<String> fileNames = new ArrayList<>();
            while (iterator.hasNext()) {
                Item item = iterator.next().get();
                fileNames.add(item.objectName());
            }
            if (CollectionUtils.isNotEmpty(fileNames)) {
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
     * @return url
     */
    public String getExpiryFileUrl(String bucketName, String objectName, int expiryTime, TimeUnit expiryTimeUnit) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryTime, expiryTimeUnit)
                            .build());
        } catch (Exception e) {
            log.error("获取文件expiry url异常：{}", e.getMessage());
            return null;
        }
    }

    public String getFileUrl(String bucketName, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("获取文件url异常：{}", e.getMessage());
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
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return true;
        } catch (Exception e) {
            log.error("上传文件异常：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 文件合并，将分块文件组成一个新的文件
     *
     * @param originBucketName 分块文件所在的桶
     * @param targetBucketName 合并文件生成文件所在的桶
     * @param fileName       文件名称
     * @return boolean
     */
    public boolean mergeFile(String originBucketName, String targetBucketName, String fileName) {
        // 获取桶内所有文件
        Iterable<Result<Item>> results = listBucketAllFile(originBucketName, true);
        if (results == null) {
            throw new ApiException(originBucketName + "bucket not have file");
        }
        List<String> fileNameList = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                fileNameList.add(item.objectName());
            }
        } catch (Exception e) {
            log.error("get bucket file exception: {}", e.getMessage());
            return false;
        }
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
            log.error("文件删除异常：{}", e.getMessage());
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
            log.error("下载文件异常：{}", e.getMessage());
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
     * @param fileName 文件名称
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

}