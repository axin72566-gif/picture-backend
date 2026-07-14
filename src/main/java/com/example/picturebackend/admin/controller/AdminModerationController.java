package com.example.picturebackend.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.admin.AdminAuth;
import com.example.picturebackend.admin.model.dto.SensitiveWordAddRequest;
import com.example.picturebackend.admin.model.dto.SensitiveWordUpdateRequest;
import com.example.picturebackend.admin.model.vo.ChatModerationLogVO;
import com.example.picturebackend.admin.model.vo.SensitiveWordVO;
import com.example.picturebackend.admin.service.AdminModerationService;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    public AdminModerationController(AdminModerationService adminModerationService) {
        this.adminModerationService = adminModerationService;
    }

    @GetMapping("/sensitive-words")
    public BaseResponse<IPage<SensitiveWordVO>> pageSensitiveWords(PageRequest pageRequest,
                                                                   HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        return ResultUtils.success(adminModerationService.pageSensitiveWords(pageRequest));
    }

    @PostMapping("/sensitive-words")
    public BaseResponse<SensitiveWordVO> addSensitiveWord(@RequestBody SensitiveWordAddRequest body,
                                                          HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        return ResultUtils.success(adminModerationService.addSensitiveWord(body));
    }

    @PutMapping("/sensitive-words/{id:\\d+}")
    public BaseResponse<SensitiveWordVO> updateSensitiveWord(@PathVariable Long id,
                                                             @RequestBody SensitiveWordUpdateRequest body,
                                                             HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        return ResultUtils.success(adminModerationService.updateSensitiveWord(id, body));
    }

    @DeleteMapping("/sensitive-words/{id:\\d+}")
    public BaseResponse<Void> deleteSensitiveWord(@PathVariable Long id, HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        adminModerationService.deleteSensitiveWord(id);
        return ResultUtils.success(null);
    }

    @GetMapping("/chat/moderation-logs")
    public BaseResponse<IPage<ChatModerationLogVO>> pageModerationLogs(
            PageRequest pageRequest,
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) Long senderId,
            HttpServletRequest request) {
        AdminAuth.requireAdminUserId(request);
        return ResultUtils.success(adminModerationService.pageModerationLogs(pageRequest, conversationId, senderId));
    }
}
