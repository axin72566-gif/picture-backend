package com.example.picturebackend.picture.model.converter;

import com.example.picturebackend.picture.entity.Picture;
import com.example.picturebackend.picture.model.vo.PictureVO;

public final class PictureConverter {

    private PictureConverter() {
    }

    public static PictureVO toVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO vo = new PictureVO();
        vo.setId(picture.getId());
        vo.setUrl(picture.getUrl());
        vo.setName(picture.getName());
        vo.setSize(picture.getSize());
        vo.setWidth(picture.getWidth());
        vo.setHeight(picture.getHeight());
        vo.setContentType(picture.getContentType());
        vo.setFormat(picture.getFormat());
        vo.setDescription(picture.getDescription());
        vo.setUserId(picture.getUserId());
        vo.setCreateTime(picture.getCreateTime());
        return vo;
    }
}
