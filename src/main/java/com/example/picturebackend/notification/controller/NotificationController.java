package com.example.picturebackend.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.notification.model.vo.NotificationVO;
import com.example.picturebackend.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/page")
    public BaseResponse<IPage<NotificationVO>> page(PageRequest pageRequest, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        IPage<NotificationVO> page = notificationService.pageNotifications(userId, pageRequest);
        return ResultUtils.success(page);
    }

    @GetMapping("/unread/count")
    public BaseResponse<Long> unreadCount(HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        return ResultUtils.success(notificationService.countUnread(userId));
    }

    @PutMapping("/{id}/read")
    public BaseResponse<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        notificationService.markRead(id, userId);
        return ResultUtils.success(null);
    }

    @PutMapping("/read/all")
    public BaseResponse<Void> markAllRead(HttpServletRequest request) {
        Long userId = requireLoginUserId(request);
        notificationService.markAllRead(userId);
        return ResultUtils.success(null);
    }

    private Long requireLoginUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return userId;
    }
}
