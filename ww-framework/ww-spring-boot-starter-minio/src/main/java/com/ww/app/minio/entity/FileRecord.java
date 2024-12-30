package com.ww.app.minio.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Data
@Document("file_record")
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
     * 1:图片 2：视频 3：音频 4：excel 5：pdf 6：doc 7：txt 8：zip 9：其他
     */
    private Integer fileType;

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
