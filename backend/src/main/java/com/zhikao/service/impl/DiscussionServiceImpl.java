package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.PageResult;
import com.zhikao.entity.*;
import com.zhikao.mapper.*;
import com.zhikao.service.DiscussionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 讨论区Service实现类
 */
@Service
public class DiscussionServiceImpl implements DiscussionService {

    @Autowired
    private DiscussionSectionMapper sectionMapper;
    @Autowired
    private DiscussionPostMapper postMapper;
    @Autowired
    private DiscussionCommentMapper commentMapper;
    @Autowired
    private DiscussionLikeMapper likeMapper;
    @Autowired
    private DiscussionFollowMapper followMapper;

    @Override
    public List<DiscussionSection> listSections() {
        LambdaQueryWrapper<DiscussionSection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiscussionSection::getStatus, 1)
                .orderByAsc(DiscussionSection::getSort);
        return sectionMapper.selectList(wrapper);
    }

    @Override
    public PageResult<DiscussionPost> listPosts(Long sectionId, String keyword, long current, long size) {
        long offset = (current - 1) * size;
        List<DiscussionPost> records = postMapper.selectPostsWithUser(offset, size, sectionId, keyword);
        long total = postMapper.countPosts(sectionId, keyword);
        return PageResult.of(records, total, size, current);
    }

    @Override
    public DiscussionPost getPost(Long postId) {
        DiscussionPost post = postMapper.selectPostDetail(postId);
        if (post != null) {
            postMapper.incrementViewCount(postId);
        }
        return post;
    }

    @Override
    @Transactional
    public DiscussionPost createPost(DiscussionPost post) {
        post.setStatus(1);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsTop(0);
        post.setIsEssence(0);
        if (post.getContentType() == null) post.setContentType(1);
        if (post.getIsAuditPassed() == null) post.setIsAuditPassed(1);
        post.setCreatedAt(new Date());
        post.setUpdatedAt(new Date());
        postMapper.insert(post);
        return post;
    }

    @Override
    public void updatePost(DiscussionPost post) {
        post.setUpdatedAt(new Date());
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId, boolean isAdmin) {
        DiscussionPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        // 管理员可删除任意帖子
        if (!isAdmin && !post.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的帖子");
        }
        post.setStatus(0);
        post.setUpdatedAt(new Date());
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public Object likePost(Long userId, Integer targetType, Long targetId) {
        LambdaQueryWrapper<DiscussionLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiscussionLike::getUserId, userId)
                .eq(DiscussionLike::getTargetType, targetType)
                .eq(DiscussionLike::getTargetId, targetId);
        DiscussionLike existing = likeMapper.selectOne(wrapper);

        if (existing != null) {
            // 取消点赞
            likeMapper.deleteById(existing.getId());
            // 更新对应目标的点赞数
            updateTargetLikeCount(targetType, targetId, false);
            Map<String, Object> result = new HashMap<>();
            result.put("liked", false);
            result.put("likeCount", getTargetLikeCount(targetType, targetId));
            return result;
        } else {
            // 点赞
            DiscussionLike like = new DiscussionLike();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            like.setCreatedAt(new Date());
            likeMapper.insert(like);
            // 更新对应目标的点赞数
            updateTargetLikeCount(targetType, targetId, true);
            Map<String, Object> result = new HashMap<>();
            result.put("liked", true);
            result.put("likeCount", getTargetLikeCount(targetType, targetId));
            return result;
        }
    }

    @Override
    public List<DiscussionComment> listComments(Long postId) {
        List<DiscussionComment> allComments = commentMapper.selectByPostId(postId);
        // 构建评论树（父子关系）
        Map<Long, DiscussionComment> commentMap = new LinkedHashMap<>();
        List<DiscussionComment> rootComments = new ArrayList<>();

        for (DiscussionComment comment : allComments) {
            comment.setChildren(new ArrayList<>());
            commentMap.put(comment.getId(), comment);
        }

        for (DiscussionComment comment : allComments) {
            if (comment.getParentId() != null && comment.getParentId() > 0) {
                DiscussionComment parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    parent.getChildren().add(comment);
                } else {
                    rootComments.add(comment);
                }
            } else {
                rootComments.add(comment);
            }
        }
        return rootComments;
    }

    @Override
    @Transactional
    public DiscussionComment addComment(Long userId, DiscussionComment comment) {
        comment.setUserId(userId);
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setCreatedAt(new Date());
        comment.setUpdatedAt(new Date());
        commentMapper.insert(comment);
        // 增加帖子评论数
        postMapper.incrementCommentCount(comment.getPostId());
        return comment;
    }

    @Override
    @Transactional
    public void topPost(Long postId, Boolean isTop) {
        DiscussionPost post = new DiscussionPost();
        post.setId(postId);
        post.setIsTop(isTop ? 1 : 0);
        post.setUpdatedAt(new Date());
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void essencePost(Long postId, Boolean isEssence) {
        DiscussionPost post = new DiscussionPost();
        post.setId(postId);
        post.setIsEssence(isEssence ? 1 : 0);
        post.setUpdatedAt(new Date());
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        DiscussionComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的评论");
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);
    }

    @Override
    @Transactional
    public DiscussionSection createSection(DiscussionSection section) {
        section.setCreatedAt(new Date());
        section.setUpdatedAt(new Date());
        if (section.getSort() == null) section.setSort(0);
        sectionMapper.insert(section);
        return section;
    }

    @Override
    @Transactional
    public void updateSection(DiscussionSection section) {
        section.setUpdatedAt(new Date());
        sectionMapper.updateById(section);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        sectionMapper.deleteById(sectionId);
    }

    /** 更新目标点赞数 */
    private void updateTargetLikeCount(Integer targetType, Long targetId, boolean increment) {
        if (targetType == 1) {
            if (increment) {
                postMapper.incrementLikeCount(targetId);
            } else {
                postMapper.decrementLikeCount(targetId);
            }
        } else if (targetType == 2) {
            DiscussionComment comment = commentMapper.selectById(targetId);
            if (comment != null) {
                int currentCount = comment.getLikeCount() != null ? comment.getLikeCount() : 0;
                comment.setLikeCount(increment ?
                        currentCount + 1 :
                        Math.max(currentCount - 1, 0));
                commentMapper.updateById(comment);
            }
        }
    }

    /** 获取目标点赞数 */
    private int getTargetLikeCount(Integer targetType, Long targetId) {
        if (targetType == 1) {
            DiscussionPost post = postMapper.selectById(targetId);
            return post != null ? post.getLikeCount() : 0;
        } else if (targetType == 2) {
            DiscussionComment comment = commentMapper.selectById(targetId);
            return comment != null ? comment.getLikeCount() : 0;
        }
        return 0;
    }
}
