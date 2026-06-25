package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.DiscussionPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 讨论区帖子Mapper
 */
@Mapper
public interface DiscussionPostMapper extends BaseMapper<DiscussionPost> {

    /**
     * 分页查询帖子列表（含用户信息）
     */
    @Select("<script>" +
            "SELECT p.*, u.username, u.nickname, s.section_name as sectionName " +
            "FROM discussion_post p " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "LEFT JOIN discussion_section s ON p.section_id = s.id " +
            "WHERE p.status = 1 " +
            "<if test='sectionId != null'>AND p.section_id = #{sectionId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND (p.title LIKE CONCAT('%',#{keyword},'%') OR p.content LIKE CONCAT('%',#{keyword},'%'))</if> " +
            "ORDER BY p.is_top DESC, p.created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<DiscussionPost> selectPostsWithUser(@Param("offset") long offset, @Param("limit") long limit,
                                              @Param("sectionId") Long sectionId,
                                              @Param("keyword") String keyword);

    /**
     * 统计帖子总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM discussion_post WHERE status = 1 " +
            "<if test='sectionId != null'>AND section_id = #{sectionId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND (title LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%'))</if> " +
            "</script>")
    long countPosts(@Param("sectionId") Long sectionId, @Param("keyword") String keyword);

    /**
     * 查询帖子详情（含用户信息）
     */
    @Select("SELECT p.*, u.username, u.nickname, s.section_name as sectionName " +
            "FROM discussion_post p " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "LEFT JOIN discussion_section s ON p.section_id = s.id " +
            "WHERE p.id = #{postId}")
    DiscussionPost selectPostDetail(@Param("postId") Long postId);

    /**
     * 增加浏览数
     */
    @Update("UPDATE discussion_post SET view_count = view_count + 1 WHERE id = #{postId}")
    void incrementViewCount(@Param("postId") Long postId);

    /**
     * 增加评论数
     */
    @Update("UPDATE discussion_post SET comment_count = comment_count + 1 WHERE id = #{postId}")
    void incrementCommentCount(@Param("postId") Long postId);

    /**
     * 减少评论数
     */
    @Update("UPDATE discussion_post SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = #{postId}")
    void decrementCommentCount(@Param("postId") Long postId);

    /**
     * 增加点赞数
     */
    @Update("UPDATE discussion_post SET like_count = like_count + 1 WHERE id = #{postId}")
    void incrementLikeCount(@Param("postId") Long postId);

    /**
     * 减少点赞数
     */
    @Update("UPDATE discussion_post SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{postId}")
    void decrementLikeCount(@Param("postId") Long postId);

    // ==================== 管理端查询 ====================

    /**
     * 管理端：分页查询所有帖子（含已删除，含审核状态筛选）
     */
    @Select("<script>" +
            "SELECT p.*, u.username, u.nickname, s.section_name as sectionName " +
            "FROM discussion_post p " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "LEFT JOIN discussion_section s ON p.section_id = s.id " +
            "WHERE 1=1 " +
            "<if test='sectionId != null'>AND p.section_id = #{sectionId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND (p.title LIKE CONCAT('%',#{keyword},'%') OR p.content LIKE CONCAT('%',#{keyword},'%'))</if> " +
            "<if test='status != null'>AND p.status = #{status}</if> " +
            "<if test='isTop != null'>AND p.is_top = #{isTop}</if> " +
            "<if test='isEssence != null'>AND p.is_essence = #{isEssence}</if> " +
            "<if test='auditStatus != null'>AND p.is_audit_passed = #{auditStatus}</if> " +
            "ORDER BY p.created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<DiscussionPost> adminListPosts(@Param("offset") long offset, @Param("limit") long limit,
                                         @Param("sectionId") Long sectionId,
                                         @Param("keyword") String keyword,
                                         @Param("status") Integer status,
                                         @Param("isTop") Integer isTop,
                                         @Param("isEssence") Integer isEssence,
                                         @Param("auditStatus") Integer auditStatus);

    /**
     * 管理端：统计帖子总数（含已删除）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM discussion_post WHERE 1=1 " +
            "<if test='sectionId != null'>AND section_id = #{sectionId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>AND (title LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%'))</if> " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "<if test='isTop != null'>AND is_top = #{isTop}</if> " +
            "<if test='isEssence != null'>AND is_essence = #{isEssence}</if> " +
            "<if test='auditStatus != null'>AND is_audit_passed = #{auditStatus}</if> " +
            "</script>")
    long adminCountPosts(@Param("sectionId") Long sectionId,
                          @Param("keyword") String keyword,
                          @Param("status") Integer status,
                          @Param("isTop") Integer isTop,
                          @Param("isEssence") Integer isEssence,
                          @Param("auditStatus") Integer auditStatus);

    /**
     * 管理端：统计各状态帖子数
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status=1 THEN 1 ELSE 0 END) as normal, " +
            "SUM(CASE WHEN status=0 THEN 1 ELSE 0 END) as deleted, " +
            "SUM(CASE WHEN is_top=1 THEN 1 ELSE 0 END) as topped, " +
            "SUM(CASE WHEN is_essence=1 THEN 1 ELSE 0 END) as essenced, " +
            "SUM(CASE WHEN is_audit_passed=0 THEN 1 ELSE 0 END) as pending " +
            "FROM discussion_post")
    Map<String, Object> adminPostStats();
}
