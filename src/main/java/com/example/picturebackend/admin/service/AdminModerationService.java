package com.example.picturebackend.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.admin.model.dto.SensitiveWordAddRequest;
import com.example.picturebackend.admin.model.dto.SensitiveWordUpdateRequest;
import com.example.picturebackend.admin.model.vo.ChatModerationLogVO;
import com.example.picturebackend.admin.model.vo.SensitiveWordVO;
import com.example.picturebackend.common.PageRequest;

public interface AdminModerationService {

    IPage<SensitiveWordVO> pageSensitiveWords(PageRequest pageRequest);

    SensitiveWordVO addSensitiveWord(SensitiveWordAddRequest request);

    SensitiveWordVO updateSensitiveWord(Long id, SensitiveWordUpdateRequest request);

    void deleteSensitiveWord(Long id);

    IPage<ChatModerationLogVO> pageModerationLogs(PageRequest pageRequest, Long conversationId, Long senderId);
}
