package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.PageRequest;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.DiscussionComment;
import com.zhikao.entity.DiscussionPost;
import com.zhikao.entity.DiscussionSection;
import com.zhikao.mapper.DiscussionCommentMapper;
import com.zhikao.mapper.DiscussionPostMapper;
import com.zhikao.mapper.DiscussionSectionMapper;
import com.zhikao.service.DiscussionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 讨论区控制器
 */
@RestController
@RequestMapping("/discuss")
public class DiscussionController {

    @Autowired
    private DiscussionService discussionService;

    @Autowired
    private com.zhikao.service.SensitiveWordService sensitiveWordService;

    @Autowired
    private DiscussionPostMapper postMapper;

    @Autowired
    private DiscussionCommentMapper commentMapper;

    @Autowired
    private DiscussionSectionMapper sectionMapper;

    /**
     * 版块列表
     */
    @GetMapping("/sections")
    public Result<List<DiscussionSection>> sections(@RequestParam(required = false, defaultValue = "false") Boolean all) {
        if (Boolean.TRUE.equals(all)) {
            // 管理端：返回所有版块（含禁用）
            return Result.success(sectionMapper.selectList(
                    new LambdaQueryWrapper<DiscussionSection>().orderByAsc(DiscussionSection::getSort)));
        }
        List<DiscussionSection> sections = discussionService.listSections();
        return Result.success(sections);
    }

    /**
     * 帖子列表
     */
    @GetMapping("/posts")
    public Result<PageResult<DiscussionPost>> posts(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        PageResult<DiscussionPost> result = discussionService.listPosts(sectionId, keyword, page, size);
        return Result.success(result);
    }

    /**
     * 帖子详情（浏览数+1）
     */
    @GetMapping("/post/{id}")
    public Result<DiscussionPost> postDetail(@PathVariable Long id) {
        DiscussionPost post = discussionService.getPost(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }
        return Result.success(post);
    }

    /**
     * 发帖
     */
    @PostMapping("/post")
    public Result<DiscussionPost> createPost(@RequestBody DiscussionPost post, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (sensitiveWordService.containsSensitive(post.getTitle()) || sensitiveWordService.containsSensitive(post.getContent())) {
            return Result.badRequest("内容包含敏感词，请修改后重试");
        }
        post.setUserId(userId);
        DiscussionPost created = discussionService.createPost(post);
        return Result.success("发帖成功", created);
    }

    /**
     * 编辑帖子（作者本人或管理员）
     */
    @PutMapping("/post/{id}")
    public Result<?> updatePost(@PathVariable Long id, @RequestBody DiscussionPost post,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        DiscussionPost existing = discussionService.getPost(id);
        if (existing == null) {
            return Result.error("帖子不存在");
        }
        // 管理员可编辑任意帖子，普通用户只能编辑自己的
        if (!"SUPER_ADMIN".equals(role) && !existing.getUserId().equals(userId)) {
            return Result.error("只能编辑自己的帖子");
        }
        post.setId(id);
        discussionService.updatePost(post);
        return Result.success("编辑成功");
    }

    /**
     * 删除帖子（作者本人或管理员）
     */
    @DeleteMapping("/post/{id}")
    public Result<?> deletePost(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        discussionService.deletePost(id, userId, "SUPER_ADMIN".equals(role));
        return Result.success("删除成功");
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/like")
    public Result<?> like(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer targetType = (Integer) params.get("targetType");
        Long targetId = Long.valueOf(params.get("targetId").toString());
        Object result = discussionService.likePost(userId, targetType, targetId);
        return Result.success(result);
    }

    /**
     * 检查当前用户是否已点赞
     */
    @GetMapping("/like/check")
    public Result<Map<String, Object>> checkLike(@RequestParam Integer targetType,
                                                   @RequestParam Long targetId,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean liked = discussionService.isLiked(userId, targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        return Result.success(result);
    }

    /**
     * 评论列表
     */
    @GetMapping("/comments/{postId}")
    public Result<List<DiscussionComment>> comments(@PathVariable Long postId,
                                                      @RequestParam(defaultValue = "1") Long page,
                                                      @RequestParam(defaultValue = "10") Long size) {
        List<DiscussionComment> comments = discussionService.listComments(postId);
        return Result.success(comments);
    }

    /**
     * 发表评论
     */
    @PostMapping("/comment")
    public Result<DiscussionComment> addComment(@RequestBody DiscussionComment comment,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (sensitiveWordService.containsSensitive(comment.getContent())) {
            return Result.badRequest("评论包含敏感词");
        }
        DiscussionComment created = discussionService.addComment(userId, comment);
        return Result.success("评论成功", created);
    }

    /**
     * 置顶/取消置顶（教师/管理员）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PutMapping("/post/{id}/top")
    public Result<?> topPost(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        Boolean isTop = params.getOrDefault("isTop", false);
        discussionService.topPost(id, isTop);
        return Result.success(isTop ? "已置顶" : "已取消置顶");
    }

    /**
     * 加精/取消加精（教师/管理员）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PutMapping("/post/{id}/essence")
    public Result<?> essencePost(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        Boolean isEssence = params.getOrDefault("isEssence", false);
        discussionService.essencePost(id, isEssence);
        return Result.success(isEssence ? "已加精" : "已取消加精");
    }

    /**
     * 删除评论（作者本人或管理员）
     */
    @DeleteMapping("/comment/{id}")
    public Result<?> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        discussionService.deleteComment(id, userId, "SUPER_ADMIN".equals(role));
        return Result.success("删除成功");
    }

    /**
     * 管理版块（管理员）：新增版块
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/section")
    public Result<DiscussionSection> createSection(@RequestBody DiscussionSection section) {
        DiscussionSection created = discussionService.createSection(section);
        return Result.success("创建成功", created);
    }

    /**
     * 管理版块（管理员）：更新版块
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/section/{id}")
    public Result<?> updateSection(@PathVariable Long id, @RequestBody DiscussionSection section) {
        section.setId(id);
        discussionService.updateSection(section);
        return Result.success("更新成功");
    }

    /**
     * 管理版块（管理员）：删除版块
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/section/{id}")
    public Result<?> deleteSection(@PathVariable Long id) {
        discussionService.deleteSection(id);
        return Result.success("删除成功");
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理端：讨论区统计概览
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> adminStats() {
        Map<String, Object> stats = new HashMap<>();

        // 帖子统计
        Map<String, Object> postStats = postMapper.adminPostStats();
        stats.put("postStats", postStats);

        // 版块统计
        long sectionTotal = sectionMapper.selectCount(null);
        long sectionActive = sectionMapper.selectCount(
                new LambdaQueryWrapper<DiscussionSection>().eq(DiscussionSection::getStatus, 1));
        Map<String, Object> sectionStats = new HashMap<>();
        sectionStats.put("total", sectionTotal);
        sectionStats.put("active", sectionActive);
        stats.put("sectionStats", sectionStats);

        // 评论统计
        long commentTotal = commentMapper.selectCount(null);
        long commentActive = commentMapper.selectCount(
                new LambdaQueryWrapper<DiscussionComment>().eq(DiscussionComment::getStatus, 1));
        Map<String, Object> commentStats = new HashMap<>();
        commentStats.put("total", commentTotal);
        commentStats.put("active", commentActive);
        stats.put("commentStats", commentStats);

        return Result.success(stats);
    }

    /**
     * 管理端：帖子列表（含已删除，多条件筛选）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/posts")
    public Result<PageResult<DiscussionPost>> adminPosts(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer isTop,
            @RequestParam(required = false) Integer isEssence,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        long offset = (page - 1) * size;
        List<DiscussionPost> records = postMapper.adminListPosts(offset, size, sectionId, keyword, status, isTop, isEssence, auditStatus);
        long total = postMapper.adminCountPosts(sectionId, keyword, status, isTop, isEssence, auditStatus);
        return Result.success(PageResult.of(records, total, size, page));
    }

    /**
     * 管理端：评论列表（跨帖子，含已删除）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @GetMapping("/admin/comments")
    public Result<PageResult<Map<String, Object>>> adminComments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long postId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        long offset = (page - 1) * size;
        List<Map<String, Object>> records = commentMapper.adminListComments(offset, size, keyword, status, postId);
        long total = commentMapper.adminCountComments(keyword, status, postId);
        return Result.success(PageResult.of(records, total, size, page));
    }

    /**
     * 管理端：审核帖子（通过/不通过）
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PutMapping("/admin/post/{id}/audit")
    public Result<?> auditPost(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer auditResult = (Integer) params.get("isAuditPassed");
        String auditMsg = (String) params.getOrDefault("auditMsg", "");

        DiscussionPost post = new DiscussionPost();
        post.setId(id);
        post.setIsAuditPassed(auditResult);
        post.setAuditMsg(auditMsg);
        post.setUpdatedAt(new Date());
        postMapper.updateById(post);
        return Result.success(auditResult == 1 ? "审核通过" : "审核不通过");
    }

    /**
     * 管理端：恢复已删除帖子
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PutMapping("/admin/post/{id}/restore")
    public Result<?> restorePost(@PathVariable Long id) {
        DiscussionPost post = new DiscussionPost();
        post.setId(id);
        post.setStatus(1);
        post.setUpdatedAt(new Date());
        postMapper.updateById(post);
        return Result.success("已恢复");
    }

    /**
     * 管理端：切换版块状态（启用/禁用）
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/admin/section/{id}/status")
    public Result<?> toggleSectionStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer status = params.getOrDefault("status", 1);
        DiscussionSection section = new DiscussionSection();
        section.setId(id);
        section.setStatus(status);
        section.setUpdatedAt(new Date());
        sectionMapper.updateById(section);
        return Result.success(status == 1 ? "已启用" : "已禁用");
    }

    /**
     * 管理端：批量删除帖子
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @DeleteMapping("/admin/posts/batch")
    public Result<?> batchDeletePosts(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.getOrDefault("ids", Collections.emptyList());
        if (ids.isEmpty()) return Result.error("未选择帖子");
        for (Long id : ids) {
            DiscussionPost post = new DiscussionPost();
            post.setId(id);
            post.setStatus(0);
            post.setUpdatedAt(new Date());
            postMapper.updateById(post);
        }
        return Result.success("已删除 " + ids.size() + " 条帖子");
    }

    // ==================== 关注接口 ====================

    /**
     * 关注/取关用户
     */
    @PostMapping("/follow")
    public Result<?> follow(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long followUserId = Long.valueOf(params.get("followUserId").toString());
        return Result.success(discussionService.followUser(userId, followUserId));
    }

    /**
     * 我关注的人
     */
    @GetMapping("/followings")
    public Result<List<Map<String, Object>>> followings(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(discussionService.listFollowings(userId));
    }

    /**
     * 关注我的人
     */
    @GetMapping("/followers")
    public Result<List<Map<String, Object>>> followers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(discussionService.listFollowers(userId));
    }
}
