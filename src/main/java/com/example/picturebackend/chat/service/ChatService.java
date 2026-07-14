package com.example.picturebackend.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.chat.model.dto.ChatMessageAddRequest;
import com.example.picturebackend.chat.model.dto.ChatReadRequest;
import com.example.picturebackend.chat.model.vo.ChatMessageVO;
import com.example.picturebackend.chat.model.vo.ConversationVO;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {

    IPage<ConversationVO> pageConversations(Long userId, PageRequest pageRequest);

    ConversationVO getSpaceConversation(Long spaceId, Long userId);

    /**
     * 获取或创建与对方的私聊会话。
     */
    ConversationVO openOrGetDm(Long userId, Long peerUserId);

    List<UserVO> listConversationMembers(Long conversationId, Long userId);

    IPage<ChatMessageVO> pageMessages(Long conversationId, Long userId, PageRequest pageRequest);

    List<ChatMessageVO> listMessagesSince(Long conversationId, Long userId, Long sinceId, int limit);

    ChatMessageVO sendMessage(Long conversationId, ChatMessageAddRequest request, Long userId);

    ChatMessageVO sendImageMessage(Long conversationId, MultipartFile file, String caption, Long replyToId,
                                   String clientMsgId, List<Long> mentionUserIds, Long userId);

    void deleteMessage(Long conversationId, Long messageId, Long operatorId);

    void markRead(Long conversationId, ChatReadRequest request, Long userId);

    Long resolveSpaceConversationId(Long spaceId);
}
