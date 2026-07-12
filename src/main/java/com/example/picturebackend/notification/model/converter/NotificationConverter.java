package com.example.picturebackend.notification.model.converter;

import com.example.picturebackend.notification.entity.Notification;
import com.example.picturebackend.notification.model.vo.NotificationVO;

public final class NotificationConverter {

    private NotificationConverter() {
    }

    public static NotificationVO toVO(Notification notification) {
        if (notification == null) {
            return null;
        }
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setPictureId(notification.getPictureId());
        vo.setCommentId(notification.getCommentId());
        vo.setContent(notification.getContent());
        vo.setIsRead(notification.getIsRead());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }
}
