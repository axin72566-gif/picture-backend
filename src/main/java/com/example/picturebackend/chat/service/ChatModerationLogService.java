package com.example.picturebackend.chat.service;

import java.util.List;

/**
 * 独立 Bean，保证拦截审计在独立事务中提交（外层发送事务回滚不影响审计）。
 */
public interface ChatModerationLogService {

    void saveBlockLog(Long conversationId, Long senderId, String messageType, String content, List<String> hits);
}
