package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.DiscussionLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 讨论区点赞Mapper
 */
@Mapper
public interface DiscussionLikeMapper extends BaseMapper<DiscussionLike> {
}
