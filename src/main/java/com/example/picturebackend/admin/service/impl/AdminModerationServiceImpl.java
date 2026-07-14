package com.example.picturebackend.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.admin.model.dto.SensitiveWordAddRequest;
import com.example.picturebackend.admin.model.dto.SensitiveWordUpdateRequest;
import com.example.picturebackend.admin.model.vo.ChatModerationLogVO;
import com.example.picturebackend.admin.model.vo.SensitiveWordVO;
import com.example.picturebackend.admin.service.AdminModerationService;
import com.example.picturebackend.chat.entity.ChatModerationLog;
import com.example.picturebackend.chat.entity.SensitiveWord;
import com.example.picturebackend.chat.mapper.ChatModerationLogMapper;
import com.example.picturebackend.chat.mapper.SensitiveWordMapper;
import com.example.picturebackend.chat.service.SensitiveWordService;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminModerationServiceImpl implements AdminModerationService {

    private static final int MAX_WORD_LENGTH = 64;

    private final SensitiveWordMapper sensitiveWordMapper;

    private final ChatModerationLogMapper chatModerationLogMapper;

    private final SensitiveWordService sensitiveWordService;

    private final UserMapper userMapper;

    public AdminModerationServiceImpl(SensitiveWordMapper sensitiveWordMapper,
                                      ChatModerationLogMapper chatModerationLogMapper,
                                      SensitiveWordService sensitiveWordService,
                                      UserMapper userMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.chatModerationLogMapper = chatModerationLogMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.userMapper = userMapper;
    }

    @Override
    public IPage<SensitiveWordVO> pageSensitiveWords(PageRequest pageRequest) {
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<SensitiveWord> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<SensitiveWord> result = sensitiveWordMapper.selectPage(page, new LambdaQueryWrapper<SensitiveWord>()
                .orderByDesc(SensitiveWord::getId));
        IPage<SensitiveWordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toWordVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public SensitiveWordVO addSensitiveWord(SensitiveWordAddRequest request) {
        String word = normalizeWord(request != null ? request.getWord() : null);
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word);
        entity.setEnabled(1);
        try {
            int rows = sensitiveWordMapper.insert(entity);
            if (rows <= 0 || entity.getId() == null) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "添加敏感词失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词已存在");
        }
        sensitiveWordService.refreshCache();
        return toWordVO(sensitiveWordMapper.selectById(entity.getId()));
    }

    @Override
    @Transactional
    public SensitiveWordVO updateSensitiveWord(Long id, SensitiveWordUpdateRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词 ID 无效");
        }
        SensitiveWord existing = sensitiveWordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词不存在");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        boolean changed = false;
        if (StringUtils.hasText(request.getWord())) {
            String word = normalizeWord(request.getWord());
            if (!word.equals(existing.getWord())) {
                existing.setWord(word);
                changed = true;
            }
        }
        if (request.getEnabled() != null) {
            int enabled = request.getEnabled() == 0 ? 0 : 1;
            if (!Objects.equals(existing.getEnabled(), enabled)) {
                existing.setEnabled(enabled);
                changed = true;
            }
        }
        if (changed) {
            try {
                sensitiveWordMapper.updateById(existing);
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词已存在");
            }
            sensitiveWordService.refreshCache();
        }
        return toWordVO(sensitiveWordMapper.selectById(id));
    }

    @Override
    @Transactional
    public void deleteSensitiveWord(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词 ID 无效");
        }
        SensitiveWord existing = sensitiveWordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词不存在");
        }
        sensitiveWordMapper.deletePhysically(id);
        sensitiveWordService.refreshCache();
    }

    @Override
    public IPage<ChatModerationLogVO> pageModerationLogs(PageRequest pageRequest, Long conversationId, Long senderId) {
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<ChatModerationLog> page = new Page<>(req.getCurrent(), req.getPageSize());
        LambdaQueryWrapper<ChatModerationLog> wrapper = new LambdaQueryWrapper<ChatModerationLog>()
                .orderByDesc(ChatModerationLog::getId);
        if (conversationId != null && conversationId > 0) {
            wrapper.eq(ChatModerationLog::getConversationId, conversationId);
        }
        if (senderId != null && senderId > 0) {
            wrapper.eq(ChatModerationLog::getSenderId, senderId);
        }
        Page<ChatModerationLog> result = chatModerationLogMapper.selectPage(page, wrapper);
        List<ChatModerationLog> records = result.getRecords();
        IPage<ChatModerationLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        if (records == null || records.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }
        Set<Long> senderIds = records.stream()
                .map(ChatModerationLog::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, UserVO> senderMap = senderIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(senderIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, UserConverter::toVO, (a, b) -> a));
        voPage.setRecords(records.stream().map(row -> {
            ChatModerationLogVO vo = new ChatModerationLogVO();
            vo.setId(row.getId());
            vo.setConversationId(row.getConversationId());
            vo.setSenderId(row.getSenderId());
            vo.setSender(senderMap.get(row.getSenderId()));
            vo.setMessageType(row.getMessageType());
            vo.setOriginalContent(row.getOriginalContent());
            vo.setHitWords(row.getHitWords());
            vo.setAction(row.getAction());
            vo.setCreateTime(row.getCreateTime());
            return vo;
        }).toList());
        return voPage;
    }

    private String normalizeWord(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词不能为空");
        }
        String word = raw.trim();
        if (word.length() > MAX_WORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "敏感词不能超过 64 个字符");
        }
        return word;
    }

    private SensitiveWordVO toWordVO(SensitiveWord entity) {
        if (entity == null) {
            return null;
        }
        SensitiveWordVO vo = new SensitiveWordVO();
        vo.setId(entity.getId());
        vo.setWord(entity.getWord());
        vo.setEnabled(entity.getEnabled());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
