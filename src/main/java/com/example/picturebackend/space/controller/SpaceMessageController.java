package com.example.picturebackend.space.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.space.model.dto.SpaceMessageAddRequest;
import com.example.picturebackend.space.model.vo.SpaceMessageVO;
import com.example.picturebackend.space.service.SpaceMessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/space")
public class SpaceMessageController {

    private final SpaceMessageService spaceMessageService;

    public SpaceMessageController(SpaceMessageService spaceMessageService) {
        this.spaceMessageService = spaceMessageService;
    }

    @GetMapping("/{id:\\d+}/messages")
    public BaseResponse<IPage<SpaceMessageVO>> pageMessages(@PathVariable Long id,
                                                            PageRequest pageRequest,
                                                            HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceMessageService.pageMessages(id, userId, pageRequest));
    }

    @PostMapping("/{id:\\d+}/messages")
    public BaseResponse<SpaceMessageVO> sendMessage(@PathVariable Long id,
                                                    @RequestBody SpaceMessageAddRequest request,
                                                    HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceMessageService.sendMessage(id, request, userId));
    }

    @DeleteMapping("/{id:\\d+}/messages/{messageId:\\d+}")
    public BaseResponse<Void> deleteMessage(@PathVariable Long id,
                                            @PathVariable Long messageId,
                                            HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        spaceMessageService.deleteMessage(id, messageId, userId);
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
