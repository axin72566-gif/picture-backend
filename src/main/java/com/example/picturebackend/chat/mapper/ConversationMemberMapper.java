package com.example.picturebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.chat.entity.ConversationMember;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {

    @Delete("DELETE FROM conversation_member WHERE conversationId = #{conversationId} AND userId = #{userId}")
    int deletePhysically(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Delete("DELETE FROM conversation_member WHERE conversationId = #{conversationId}")
    int deleteAllByConversationIdPhysically(@Param("conversationId") Long conversationId);

    @Select("SELECT userId FROM conversation_member WHERE conversationId = #{conversationId} AND isDelete = 0")
    List<Long> selectUserIdsByConversationId(@Param("conversationId") Long conversationId);
}
