package com.ww.mall.minio.bo;

import lombok.Data;

@Data
public class ChunkFileMergeReqBO {

    private String uploadId;

    private String fileMd5;

    private String fileName;

}
