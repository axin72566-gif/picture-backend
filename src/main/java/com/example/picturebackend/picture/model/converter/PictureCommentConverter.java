package com.example.picturebackend.picture.model.converter;

import com.example.picturebackend.picture.entity.PictureComment;
import com.example.picturebackend.picture.model.vo.PictureCommentVO;

public final class PictureCommentConverter {

    private PictureCommentConverter() {
    }

    public static PictureCommentVO toVO(PictureComment comment) {
        if (comment == null) {
            return null;
        }
        PictureCommentVO vo = new PictureCommentVO();
        vo.setId(comment.getId());
        vo.setPictureId(comment.getPictureId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
