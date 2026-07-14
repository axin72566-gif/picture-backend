package com.example.picturebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.picturebackend.chat.entity.SensitiveWord;
import com.example.picturebackend.chat.mapper.SensitiveWordMapper;
import com.example.picturebackend.chat.service.ChatModerationLogService;
import com.example.picturebackend.chat.service.SensitiveWordService;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    public static final String REDIS_KEY = "chat:sensitive:words";

    private final SensitiveWordMapper sensitiveWordMapper;

    private final ChatModerationLogService chatModerationLogService;

    private final StringRedisTemplate stringRedisTemplate;

    public SensitiveWordServiceImpl(SensitiveWordMapper sensitiveWordMapper,
                                    ChatModerationLogService chatModerationLogService,
                                    StringRedisTemplate stringRedisTemplate) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.chatModerationLogService = chatModerationLogService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void assertCleanOrBlock(Long conversationId, Long senderId, String messageType, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        List<String> hits = findHits(content);
        if (hits.isEmpty()) {
            return;
        }
        chatModerationLogService.saveBlockLog(conversationId, senderId, messageType, content, hits);
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息包含敏感内容");
    }

    @Override
    public List<String> findHits(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        Set<String> words = loadEnabledWords();
        if (words.isEmpty()) {
            return List.of();
        }
        String haystack = content.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        for (String word : words) {
            if (!StringUtils.hasText(word)) {
                continue;
            }
            String needle = word.toLowerCase(Locale.ROOT);
            if (haystack.contains(needle)) {
                hits.add(word);
            }
        }
        return hits;
    }

    @Override
    public void refreshCache() {
        stringRedisTemplate.delete(REDIS_KEY);
        Set<String> words = loadFromDb();
        if (!words.isEmpty()) {
            stringRedisTemplate.opsForSet().add(REDIS_KEY, words.toArray(String[]::new));
        }
        log.info("敏感词缓存已刷新, size={}", words.size());
    }

    private Set<String> loadEnabledWords() {
        Boolean exists = stringRedisTemplate.hasKey(REDIS_KEY);
        if (Boolean.TRUE.equals(exists)) {
            Set<String> cached = stringRedisTemplate.opsForSet().members(REDIS_KEY);
            if (cached == null || cached.isEmpty()) {
                return Set.of();
            }
            return cached.stream()
                    .filter(StringUtils::hasText)
                    .filter(w -> !"\u0000".equals(w))
                    .collect(Collectors.toSet());
        }
        Set<String> fromDb = loadFromDb();
        if (!fromDb.isEmpty()) {
            stringRedisTemplate.opsForSet().add(REDIS_KEY, fromDb.toArray(String[]::new));
        } else {
            // 空词库也写 key，避免反复打库；用空 set 无法表示，放一个不可匹配占位
            stringRedisTemplate.opsForSet().add(REDIS_KEY, "\u0000");
        }
        return fromDb;
    }

    private Set<String> loadFromDb() {
        List<SensitiveWord> list = sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getEnabled, 1));
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        return list.stream()
                .map(SensitiveWord::getWord)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
