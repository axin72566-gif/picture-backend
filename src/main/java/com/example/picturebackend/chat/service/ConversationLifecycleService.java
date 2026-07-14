package com.example.picturebackend.chat.service;

import com.example.picturebackend.chat.entity.Conversation;

public interface ConversationLifecycleService {

    Conversation createSpaceConversation(Long spaceId, Long creatorUserId);

    void addMember(Long spaceId, Long userId);

    void removeMember(Long spaceId, Long userId);

    void dissolveSpaceConversation(Long spaceId);

    /**
     * 获取或创建两人私聊会话。
     *
     * @return created=true 表示本次新建
     */
    DmOpenResult openOrGetDm(Long userId, Long peerUserId);

    record DmOpenResult(Conversation conversation, boolean created) {
    }
}
