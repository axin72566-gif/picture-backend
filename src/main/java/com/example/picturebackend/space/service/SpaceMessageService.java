package com.example.picturebackend.space.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.space.model.dto.SpaceMessageAddRequest;
import com.example.picturebackend.space.model.vo.SpaceMessageVO;

public interface SpaceMessageService {

    IPage<SpaceMessageVO> pageMessages(Long spaceId, Long userId, PageRequest pageRequest);

    SpaceMessageVO sendMessage(Long spaceId, SpaceMessageAddRequest request, Long userId);

    void deleteMessage(Long spaceId, Long messageId, Long operatorId);
}
