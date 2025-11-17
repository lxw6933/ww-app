package com.ww.mall.product.entity.test.comment;

import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.product.enums.CommentType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("product_comment")
public class ProductComment extends BaseDoc {

    /**
     * 默认匿名昵称
     */
    public static final String NICKNAME_ANONYMOUS = "匿名用户";

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

}
