package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 收藏项实体
 */
@Data
@TableName("favorite_item")
public class FavoriteItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 收藏夹ID */
    private Long folderId;

    /** 收藏类型：1题目 2试卷 3学习资料 4视频链接 */
    private Integer itemType;

    /** 关联ID */
    private Long itemId;

    /** 标题 */
    private String itemTitle;

    /** 链接地址 */
    private String itemUrl;

    /** 个人笔记 */
    private String note;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
