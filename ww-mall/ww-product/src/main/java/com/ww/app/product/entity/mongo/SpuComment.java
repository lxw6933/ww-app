package com.ww.app.product.entity.mongo;

import com.ww.app.product.enums.CommentType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@Document("t_spu_comment")
public class SpuComment {

    @Id
    private String id;

    /**
     * sku_id
     */
    private Long skuId;

    /**
     * spu_id
     */
    private Long spuId;

    /**
     * 商品名字
     */
    private String spuName;

    /**
     * 会员昵称
     */
    private String memberNickName;

    /**
     * 星级
     */
    private Boolean star;

    /**
     * 会员ip
     */
    private String memberIp;

    /**
     * 显示状态[0-不显示，1-显示]
     */
    private Boolean status;

    /**
     * 购买时属性组合
     */
    private String spuAttributes;

    /**
     * 点赞数
     */
    private Integer likesCount;

    /**
     * 回复数
     */
    private Integer replyCount;

    /**
     * 评论图片/视频[json数据；[{type:文件类型,url:资源路径}]]
     */
    private String resources;

    /**
     * 内容
     */
    private String content;

    /**
     * 用户头像
     */
    private String memberIcon;

    /**
     * 评论类型[对商品的直接评论、对评论的回复]
     */
    private CommentType commentType;

    /**
     * 创建时间
     */
    private String createTime;

}
