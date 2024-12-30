package com.ww.app.minio.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class CreateMultipartUploadResultVO implements Serializable {

    /**
     * 上传id
     */
    private String uploadId;

    /**
     * 分片地址
     */
    private Map<String, Object> chunkFileList;

}
