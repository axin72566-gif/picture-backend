package com.example.picturebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select({
            "<script>",
            "SELECT * FROM chat_message",
            "WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<ChatMessage> selectBatchIdsIncludeDeleted(@Param("ids") Collection<Long> ids);

    @Select("""
            SELECT COUNT(*) FROM chat_message
            WHERE conversationId = #{conversationId}
              AND id > #{lastReadMessageId}
              AND senderId <> #{userId}
              AND isDelete = 0
            """)
    long countUnread(@Param("conversationId") Long conversationId,
                     @Param("lastReadMessageId") Long lastReadMessageId,
                     @Param("userId") Long userId);
}
