package com.example.picturebackend.chat.service;

import com.example.picturebackend.chat.entity.Conversation;

public interface ConversationLifecycleService {

    Conversation createSpaceConversation(Long spaceId, Long creatorUserId);

    void addMember(Long spaceId, Long userId);

    void removeMember(Long spaceId, Long userId);

    void dissolveSpaceConversation(Long spaceId);
}
