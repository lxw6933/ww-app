package com.ww.mall.third.result.edu.res;

import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2023-03-29- 11:04
 * @description:
 */
@Data
public class MpnListResultVO {

    /**
     * mpn数据列表
     */
    private List<EduProduct> products;

    /**
     * 示例：success
     */
    private String status;

    @Data
    class EduProduct {

        /**
         * 示例：Mac Mini
         */
        private String sublob;

        /**
         * 示例：MGPC3CH/A
         */
        private String mpn;

        /**
         * 示例：Mac
         */
        private String lob;

    }

}
