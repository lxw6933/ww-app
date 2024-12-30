package com.ww.app.third.edu.res;

import lombok.Data;

/**
 * @author lwl
 * @create 2023-04-03- 09:55
 * @description:
 */
@Data
public class UploadPicturesResultVO {

    /**
     * 示例：
     * c74162ffaead43d591138eb996ae66ee
     */
    private String image_no;

    /**
     * 示例：http://eduorderonline.kitapps.com.cn//image/download2/node1/2021/08/17/14//71793.png
     */
    private String image_url;

}
