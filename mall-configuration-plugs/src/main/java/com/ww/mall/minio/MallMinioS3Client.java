package com.ww.mall.minio;

import com.google.common.collect.Multimap;
import io.minio.CreateMultipartUploadResponse;
import io.minio.ListPartsResponse;
import io.minio.MinioAsyncClient;
import io.minio.ObjectWriteResponse;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Part;
import lombok.SneakyThrows;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author ww
 * @create 2024-04-24- 13:31
 * @description:
 */
public class MallMinioS3Client extends MinioAsyncClient {

    public MallMinioS3Client(MinioAsyncClient client) {
        super(client);
    }

    @SneakyThrows
    @Override
    public CreateMultipartUploadResponse createMultipartUpload(String bucketName, String region, String fileName, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException {
        return super.createMultipartUploadAsync(bucketName, region, fileName, headers, extraQueryParams).get();
    }

    public ObjectWriteResponse mergeMultipartUploadFile(String bucketName, String region, String objectName, String uploadId, Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws Exception {
        return super.completeMultipartUploadAsync(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams).get();
    }

    public ListPartsResponse listMultipart(String bucketName, String region, String objectName, Integer maxParts, Integer partNumberMarker, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws Exception {
        return super.listPartsAsync(bucketName, region, objectName, maxParts, partNumberMarker, uploadId, extraHeaders, extraQueryParams).get();
    }
}
