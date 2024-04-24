package com.ww.mall.minio.s3;

import com.google.common.collect.Multimap;
import io.minio.CreateMultipartUploadResponse;
import io.minio.ListPartsResponse;
import io.minio.MinioAsyncClient;
import io.minio.ObjectWriteResponse;
import io.minio.messages.Part;

/**
 * @author ww
 * @create 2024-04-24- 13:31
 * @description:
 */
public class MallMinioClient extends MinioAsyncClient {

    public MallMinioClient(MinioAsyncClient client) {
        super(client);
    }

    /**
     * 分片上传
     *
     * @param bucketName bucketName
     * @param region 区域
     * @param fileName 分片文件名称
     * @param headers headers
     * @param extraQueryParams extraParams
     * @return uploadId
     */
    public String chunkFileUpload(String bucketName, String region, String fileName, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws Exception {
        CreateMultipartUploadResponse response = this.createMultipartUploadAsync(bucketName, region, fileName, headers, extraQueryParams).get();
        return response.result().uploadId();
    }

    /**
     * 合并分片
     *
     * @param bucketName       String   桶名称
     * @param region           String
     * @param objectName       String   文件名称
     * @param uploadId         String   上传的 uploadId
     * @param parts            Part[]   分片集合
     * @param extraHeaders     Multimap<String, String>
     * @param extraQueryParams Multimap<String, String>
     * @return ObjectWriteResponse
     */
    public ObjectWriteResponse mergeChunkFile(String bucketName, String region, String objectName, String uploadId, Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws Exception {
        return this.completeMultipartUploadAsync(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams).get();
    }

    /**
     * 查询当前上传后的分片信息
     *
     * @param bucketName       String   桶名称
     * @param region           String
     * @param objectName       String   文件名称
     * @param maxParts         Integer  分片数量
     * @param partNumberMarker Integer  分片起始值
     * @param uploadId         String   上传的 uploadId
     * @param extraHeaders     Multimap<String, String>
     * @param extraQueryParams Multimap<String, String>
     * @return ListPartsResponse
     */
    public ListPartsResponse listMultipart(String bucketName, String region, String objectName, Integer maxParts, Integer partNumberMarker, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws Exception {
        return this.listPartsAsync(bucketName, region, objectName, maxParts, partNumberMarker, uploadId, extraHeaders, extraQueryParams).get();
    }
}
