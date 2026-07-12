package com.example.picturebackend.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.notification.entity.Notification;
import com.example.picturebackend.notification.mapper.NotificationMapper;
import com.example.picturebackend.notification.model.converter.NotificationConverter;
import com.example.picturebackend.notification.model.vo.NotificationVO;
import com.example.picturebackend.notification.service.NotificationService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_CONTENT_LENGTH = 100;

    private final NotificationMapper notificationMapper;

    private final UserMapper userMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper, UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void create(Long receiverId, Long senderId, String type, Long pictureId, Long commentId, Long spaceId,
                       String content) {
        if (receiverId == null || senderId == null || !StringUtils.hasText(type)) {
            return;
        }
        if (Objects.equals(receiverId, senderId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setPictureId(pictureId);
        notification.setCommentId(commentId);
        notification.setSpaceId(spaceId);
        notification.setContent(truncateContent(content));
        notification.setIsRead(0);

        int rows = notificationMapper.insert(notification);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "创建通知失败");
        }
    }

    @Override
    public IPage<NotificationVO> pageNotifications(Long receiverId, PageRequest request) {
        if (receiverId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        PageRequest pageRequest = request != null ? request : new PageRequest();
        Page<Notification> page = new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize());
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsDelete, 0)
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationMapper.selectPage(page, wrapper);
        return toVoPage(result);
    }

    @Override
    public long countUnread(Long receiverId) {
        if (receiverId == null) {
            return 0;
        }
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, 0)
                .eq(Notification::getIsDelete, 0));
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long receiverId) {
        if (notificationId == null || notificationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "通知 ID 不能为空");
        }
        if (receiverId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "通知不存在");
        }
        if (!Objects.equals(notification.getReceiverId(), receiverId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能操作自己的通知");
        }
        if (Objects.equals(notification.getIsRead(), 1)) {
            return;
        }

        Notification update = new Notification();
        update.setId(notificationId);
        update.setIsRead(1);
        int rows = notificationMapper.updateById(update);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "标记已读失败");
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long receiverId) {
        if (receiverId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, 0)
                .eq(Notification::getIsDelete, 0)
                .set(Notification::getIsRead, 1));
    }

    private IPage<NotificationVO> toVoPage(Page<Notification> result) {
        List<Notification> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            IPage<NotificationVO> emptyPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Set<Long> senderIds = records.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> senderMap = senderIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, UserConverter::toVO, (a, b) -> a));

        List<NotificationVO> voList = records.stream().map(notification -> {
            NotificationVO vo = NotificationConverter.toVO(notification);
            vo.setSender(senderMap.get(notification.getSenderId()));
            return vo;
        }).toList();

        IPage<NotificationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private static String truncateContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.length() <= MAX_CONTENT_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_CONTENT_LENGTH);
    }
}
