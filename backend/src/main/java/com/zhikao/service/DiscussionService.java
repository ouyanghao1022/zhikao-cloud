package com.zhikao.service;

import com.zhikao.common.PageResult;
import com.zhikao.entity.DiscussionComment;
import com.zhikao.entity.DiscussionPost;
import com.zhikao.entity.DiscussionSection;

import java.util.List;

/**
 * 讨论区Service接口
 */
public interface DiscussionService {

    /** 获取版块列表 */
    List<DiscussionSection> listSections();

    /** 帖子列表（分页） */
    PageResult<DiscussionPost> listPosts(Long sectionId, String keyword, long current, long size);

    /** 获取帖子详情 */
    DiscussionPost getPost(Long postId);

    /** 发帖 */
    DiscussionPost createPost(DiscussionPost post);

    /** 编辑帖子 */
    void updatePost(DiscussionPost post);

    /** 删除帖子（isAdmin 可跳过作者校验） */
    void deletePost(Long postId, Long userId, boolean isAdmin);

    /** 点赞/取消点赞 */
    Object likePost(Long userId, Integer targetType, Long targetId);

    /** 是否已点赞 */
    boolean isLiked(Long userId, Integer targetType, Long targetId);

    /** 评论列表 */
    List<DiscussionComment> listComments(Long postId);

    /** 添加评论 */
    DiscussionComment addComment(Long userId, DiscussionComment comment);

    /** 置顶/取消置顶 */
    void topPost(Long postId, Boolean isTop);

    /** 加精/取消加精 */
    void essencePost(Long postId, Boolean isEssence);

    /** 删除评论（isAdmin 可跳过作者校验） */
    void deleteComment(Long commentId, Long userId, boolean isAdmin);

    /** 管理版块 */
    DiscussionSection createSection(DiscussionSection section);
    void updateSection(DiscussionSection section);
    void deleteSection(Long sectionId);

    /** 关注/取关用户（toggle），返回 {followed:true/false} */
    Object followUser(Long userId, Long followUserId);

    /** 我关注的人列表 */
    java.util.List<java.util.Map<String, Object>> listFollowings(Long userId);

    /** 关注我的人列表 */
    java.util.List<java.util.Map<String, Object>> listFollowers(Long userId);

    /** 是否已关注 */
    boolean isFollowing(Long userId, Long followUserId);
}
