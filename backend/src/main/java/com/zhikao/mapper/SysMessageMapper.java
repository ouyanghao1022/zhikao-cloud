package com.zhikao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {

    /** 批量标记为已读 */
    @Update("UPDATE sys_message SET is_read = 1 WHERE receiver_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);

    /** 获取未读消息数 */
    @Select("SELECT COUNT(*) FROM sys_message WHERE receiver_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);
}
