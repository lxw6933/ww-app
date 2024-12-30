package com.ww.app.minio.bo;

import lombok.Data;

@Data
public class ChunkFileUploadReqBO {

    /**
     * 分片数量大小
     */
    private Integer chunkSize;

    /**
     * 分片上传文件名称
     */
    private String fileName;

    /**
     * md5
     */
    private String fileMd5;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 分片uploadId
     */
    private String uploadId;

}
