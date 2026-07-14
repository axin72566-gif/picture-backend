package com.example.picturebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.chat.entity.ChatMessageMention;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChatMessageMentionMapper extends BaseMapper<ChatMessageMention> {

    @Select("<script>"
            + "SELECT * FROM chat_message_mention WHERE messageId IN "
            + "<foreach collection='messageIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<ChatMessageMention> selectByMessageIds(@Param("messageIds") List<Long> messageIds);
}
