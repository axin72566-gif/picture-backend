package com.example.picturebackend.space.model.converter;

import com.example.picturebackend.space.entity.SpaceMessage;
import com.example.picturebackend.space.model.vo.SpaceMessageVO;

public final class SpaceMessageConverter {

    private SpaceMessageConverter() {
    }

    public static SpaceMessageVO toVO(SpaceMessage message) {
        if (message == null) {
            return null;
        }
        SpaceMessageVO vo = new SpaceMessageVO();
        vo.setId(message.getId());
        vo.setSpaceId(message.getSpaceId());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
