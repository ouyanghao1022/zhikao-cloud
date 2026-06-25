package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.DiscussionComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 讨论区评论Mapper
 */
@Mapper
public interface DiscussionCommentMapper extends BaseMapper<DiscussionComment> {

    /**
     * 查询帖子的评论列表（含用户信息）
     */
    @Select("SELECT c.*, u.username " +
            "FROM discussion_comment c " +
            "LEFT JOIN sys_user u ON c.user_id = u.id " +
            "WHERE c.post_id = #{postId} AND c.status = 1 " +
            "ORDER BY c.created_at ASC")
    List<DiscussionComment> selectByPostId(@Param("postId") Long postId);

    /**
     * 管理端：分页查询所有评论（含已删除，含帖子标题）
     */
    @Select("<script>" +
            "SELECT c.*, u.username, p.title as post_title " +
            "FROM discussion_comment c " +
            "LEFT JOIN sys_user u ON c.user_id = u.id " +
            "LEFT JOIN discussion_post p ON c.post_id = p.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>AND c.content LIKE CONCAT('%',#{keyword},'%')</if> " +
            "<if test='status != null'>AND c.status = #{status}</if> " +
            "<if test='postId != null'>AND c.post_id = #{postId}</if> " +
            "ORDER BY c.created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> adminListComments(@Param("offset") long offset, @Param("limit") long limit,
                                                  @Param("keyword") String keyword,
                                                  @Param("status") Integer status,
                                                  @Param("postId") Long postId);

    /**
     * 管理端：统计评论总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM discussion_comment WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>AND content LIKE CONCAT('%',#{keyword},'%')</if> " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "<if test='postId != null'>AND post_id = #{postId}</if> " +
            "</script>")
    long adminCountComments(@Param("keyword") String keyword,
                             @Param("status") Integer status,
                             @Param("postId") Long postId);
}
