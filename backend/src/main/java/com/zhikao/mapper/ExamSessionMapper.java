package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.ExamSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试记录Mapper
 */
@Mapper
public interface ExamSessionMapper extends BaseMapper<ExamSession> {
}
