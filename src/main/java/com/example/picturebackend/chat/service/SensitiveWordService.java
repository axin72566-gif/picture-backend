package com.example.picturebackend.chat.service;

import java.util.List;

public interface SensitiveWordService {

    /**
     * 扫描文本；命中则写审计并抛出业务异常。空文本直接放行。
     */
    void assertCleanOrBlock(Long conversationId, Long senderId, String messageType, String content);

    List<String> findHits(String content);

    void refreshCache();
}
