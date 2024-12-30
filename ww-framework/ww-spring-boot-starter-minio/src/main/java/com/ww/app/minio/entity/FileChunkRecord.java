package com.ww.app.minio.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileChunkRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 文件md5
     */
    private String md5;

    /**
     * 分片号
     */
    private Integer chunk;

    /**
     * 分片文件是否上传成功
     */
    private Boolean uploadStatus;

}
