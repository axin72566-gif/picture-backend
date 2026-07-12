package com.example.picturebackend.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.notification.model.vo.NotificationVO;

public interface NotificationService {

    /**
     * 创建一条站内通知。receiverId 与 senderId 相同时跳过。
     */
    void create(Long receiverId, Long senderId, String type, Long pictureId, Long commentId, Long spaceId, String content);

    /**
     * 兼容无 spaceId 的调用。
     */
    default void create(Long receiverId, Long senderId, String type, Long pictureId, Long commentId, String content) {
        create(receiverId, senderId, type, pictureId, commentId, null, content);
    }

    IPage<NotificationVO> pageNotifications(Long receiverId, PageRequest request);

    long countUnread(Long receiverId);

    void markRead(Long notificationId, Long receiverId);

    void markAllRead(Long receiverId);
}
