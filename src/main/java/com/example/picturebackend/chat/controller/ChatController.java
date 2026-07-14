package com.example.picturebackend.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.chat.model.dto.ChatMessageAddRequest;
import com.example.picturebackend.chat.model.dto.ChatReadRequest;
import com.example.picturebackend.chat.model.dto.DmOpenRequest;
import com.example.picturebackend.chat.model.vo.ChatMessageVO;
import com.example.picturebackend.chat.model.vo.ConversationVO;
import com.example.picturebackend.chat.service.ChatService;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.user.model.vo.UserVO;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public BaseResponse<IPage<ConversationVO>> pageConversations(PageRequest pageRequest,
                                                                 HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(chatService.pageConversations(userId, pageRequest));
    }

    @GetMapping("/conversations/by-space/{spaceId:\\d+}")
    public BaseResponse<ConversationVO> getBySpace(@PathVariable Long spaceId,
                                                   HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(chatService.getSpaceConversation(spaceId, userId));
    }

    @PostMapping("/conversations/dm")
    public BaseResponse<ConversationVO> openOrGetDm(@RequestBody DmOpenRequest request,
                                                    HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        Long peerUserId = request != null ? request.getPeerUserId() : null;
        return ResultUtils.success(chatService.openOrGetDm(userId, peerUserId));
    }

    @GetMapping("/conversations/{id:\\d+}/members")
    public BaseResponse<List<UserVO>> listMembers(@PathVariable Long id,
                                                  HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(chatService.listConversationMembers(id, userId));
    }

    @GetMapping("/conversations/{id:\\d+}/messages")
    public BaseResponse<?> pageOrSinceMessages(@PathVariable Long id,
                                               PageRequest pageRequest,
                                               @RequestParam(required = false) Long sinceId,
                                               @RequestParam(required = false, defaultValue = "100") Integer limit,
                                               HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        if (sinceId != null) {
            List<ChatMessageVO> list = chatService.listMessagesSince(id, userId, sinceId, limit == null ? 100 : limit);
            return ResultUtils.success(list);
        }
        return ResultUtils.success(chatService.pageMessages(id, userId, pageRequest));
    }

    @PostMapping("/conversations/{id:\\d+}/messages")
    public BaseResponse<ChatMessageVO> sendMessage(@PathVariable Long id,
                                                   @RequestBody ChatMessageAddRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(chatService.sendMessage(id, request, userId));
    }

    @PostMapping("/conversations/{id:\\d+}/messages/image")
    public BaseResponse<ChatMessageVO> sendImageMessage(@PathVariable Long id,
                                                        @RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false) String caption,
                                                        @RequestParam(required = false) Long replyToId,
                                                        @RequestParam(required = false) String clientMsgId,
                                                        @RequestParam(required = false) Long[] mentionUserIds,
                                                        HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        List<Long> mentions = mentionUserIds == null
                ? null
                : Arrays.stream(mentionUserIds).filter(uid -> uid != null && uid > 0).collect(Collectors.toList());
        return ResultUtils.success(chatService.sendImageMessage(
                id, file, caption, replyToId, clientMsgId, mentions, userId));
    }

    @DeleteMapping("/conversations/{id:\\d+}/messages/{messageId:\\d+}")
    public BaseResponse<Void> deleteMessage(@PathVariable Long id,
                                            @PathVariable Long messageId,
                                            HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        chatService.deleteMessage(id, messageId, userId);
        return ResultUtils.success(null);
    }

    @PutMapping("/conversations/{id:\\d+}/read")
    public BaseResponse<Void> markRead(@PathVariable Long id,
                                       @RequestBody ChatReadRequest request,
                                       HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        chatService.markRead(id, request, userId);
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
