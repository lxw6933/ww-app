package com.ww.mall.minio.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件md5
     */
    private String fileMd5;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private String fileSize;

    /**
     * 是否上传成功
     */
    private Boolean uploadStatus;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

}
