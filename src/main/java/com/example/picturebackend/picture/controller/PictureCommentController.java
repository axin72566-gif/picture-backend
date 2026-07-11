package com.example.picturebackend.picture.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.picture.model.dto.PictureCommentAddRequest;
import com.example.picturebackend.picture.model.dto.PictureCommentQueryRequest;
import com.example.picturebackend.picture.model.vo.PictureCommentVO;
import com.example.picturebackend.picture.service.PictureCommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/picture")
public class PictureCommentController {

    private final PictureCommentService pictureCommentService;

    public PictureCommentController(PictureCommentService pictureCommentService) {
        this.pictureCommentService = pictureCommentService;
    }

    @PostMapping("/{pictureId}/comment")
    public BaseResponse<PictureCommentVO> addComment(@PathVariable Long pictureId,
                                                     @RequestBody PictureCommentAddRequest request,
                                                     HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        PictureCommentVO vo = pictureCommentService.addComment(pictureId, request, userId);
        return ResultUtils.success(vo);
    }

    @GetMapping("/{pictureId}/comments")
    public BaseResponse<IPage<PictureCommentVO>> pageRootComments(@PathVariable Long pictureId,
                                                                  PictureCommentQueryRequest request) {
        IPage<PictureCommentVO> page = pictureCommentService.pageRootComments(pictureId, request);
        return ResultUtils.success(page);
    }

    @GetMapping("/comment/{rootId}/replies")
    public BaseResponse<IPage<PictureCommentVO>> pageReplies(@PathVariable Long rootId,
                                                             PictureCommentQueryRequest request) {
        IPage<PictureCommentVO> page = pictureCommentService.pageReplies(rootId, request);
        return ResultUtils.success(page);
    }

    @DeleteMapping("/comment/{id}")
    public BaseResponse<Void> deleteComment(@PathVariable Long id,
                                            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        pictureCommentService.deleteComment(id, userId);
        return ResultUtils.success(null);
    }
}
