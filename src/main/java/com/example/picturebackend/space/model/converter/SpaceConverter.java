package com.example.picturebackend.space.model.converter;

import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.model.vo.SpaceVO;

public final class SpaceConverter {

    private SpaceConverter() {
    }

    public static SpaceVO toVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO vo = new SpaceVO();
        vo.setId(space.getId());
        vo.setName(space.getName());
        vo.setDescription(space.getDescription());
        vo.setOwnerId(space.getOwnerId());
        vo.setCreateTime(space.getCreateTime());
        vo.setUpdateTime(space.getUpdateTime());
        return vo;
    }
}
