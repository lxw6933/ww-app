package com.ww.mall.minio.s3;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2024-04-24- 13:46
 * @description:
 */
@Data
public class FileUploadInfo {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @NotBlank(message = "文件类型不能为空")
    private String contentType;

    @NotNull(message = "分片总数量不能为空")
    private Integer chunkNumber;

    @NotBlank(message = "uploadId不能为空")
    private String uploadId;

    private Long chunkSize;

    private String fileMd5;

    private String fileType;

    private List<Integer> chunkUploadedList;

}
