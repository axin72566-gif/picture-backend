package com.example.picturebackend.picture.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.picture.model.dto.PictureCommentAddRequest;
import com.example.picturebackend.picture.model.dto.PictureCommentQueryRequest;
import com.example.picturebackend.picture.model.vo.PictureCommentVO;

public interface PictureCommentService {

    PictureCommentVO addComment(Long pictureId, PictureCommentAddRequest request, Long userId);

    IPage<PictureCommentVO> pageRootComments(Long pictureId, PictureCommentQueryRequest request);

    IPage<PictureCommentVO> pageReplies(Long rootId, PictureCommentQueryRequest request);

    void deleteComment(Long commentId, Long userId);
}
