package com.example.picturebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.chat.constant.ChatMessageType;
import com.example.picturebackend.chat.constant.ConversationType;
import com.example.picturebackend.chat.entity.ChatMessage;
import com.example.picturebackend.chat.entity.ChatMessageMention;
import com.example.picturebackend.chat.entity.Conversation;
import com.example.picturebackend.chat.entity.ConversationMember;
import com.example.picturebackend.chat.event.ChatEventPublisher;
import com.example.picturebackend.chat.mapper.ChatMessageMapper;
import com.example.picturebackend.chat.mapper.ChatMessageMentionMapper;
import com.example.picturebackend.chat.mapper.ConversationMapper;
import com.example.picturebackend.chat.mapper.ConversationMemberMapper;
import com.example.picturebackend.chat.model.converter.ChatMessageConverter;
import com.example.picturebackend.chat.model.dto.ChatMessageAddRequest;
import com.example.picturebackend.chat.model.dto.ChatReadRequest;
import com.example.picturebackend.chat.model.vo.ChatEvent;
import com.example.picturebackend.chat.model.vo.ChatMessageReplyToVO;
import com.example.picturebackend.chat.model.vo.ChatMessageVO;
import com.example.picturebackend.chat.model.vo.ConversationVO;
import com.example.picturebackend.chat.service.ChatService;
import com.example.picturebackend.chat.service.ConversationLifecycleService;
import com.example.picturebackend.chat.service.SensitiveWordService;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.config.CosProperties;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.notification.constant.NotificationType;
import com.example.picturebackend.notification.service.NotificationService;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.entity.Space;
import com.example.picturebackend.space.entity.SpaceMember;
import com.example.picturebackend.space.mapper.SpaceMapper;
import com.example.picturebackend.space.service.SpaceService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private static final int DEFAULT_SINCE_LIMIT = 100;

    private static final int MAX_MENTIONS = 20;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final ConversationMapper conversationMapper;

    private final ConversationMemberMapper conversationMemberMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final ChatMessageMentionMapper chatMessageMentionMapper;

    private final SpaceMapper spaceMapper;

    private final UserMapper userMapper;

    private final SpaceService spaceService;

    private final ConversationLifecycleService conversationLifecycleService;

    private final ChatEventPublisher chatEventPublisher;

    private final NotificationService notificationService;

    private final SensitiveWordService sensitiveWordService;

    private final COSClient cosClient;

    private final CosProperties cosProperties;

    public ChatServiceImpl(ConversationMapper conversationMapper,
                           ConversationMemberMapper conversationMemberMapper,
                           ChatMessageMapper chatMessageMapper,
                           ChatMessageMentionMapper chatMessageMentionMapper,
                           SpaceMapper spaceMapper,
                           UserMapper userMapper,
                           SpaceService spaceService,
                           ConversationLifecycleService conversationLifecycleService,
                           ChatEventPublisher chatEventPublisher,
                           NotificationService notificationService,
                           SensitiveWordService sensitiveWordService,
                           COSClient cosClient,
                           CosProperties cosProperties) {
        this.conversationMapper = conversationMapper;
        this.conversationMemberMapper = conversationMemberMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMessageMentionMapper = chatMessageMentionMapper;
        this.spaceMapper = spaceMapper;
        this.userMapper = userMapper;
        this.spaceService = spaceService;
        this.conversationLifecycleService = conversationLifecycleService;
        this.chatEventPublisher = chatEventPublisher;
        this.notificationService = notificationService;
        this.sensitiveWordService = sensitiveWordService;
        this.cosClient = cosClient;
        this.cosProperties = cosProperties;
    }

    @Override
    public IPage<ConversationVO> pageConversations(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<ConversationMember> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<ConversationMember> memberPage = conversationMemberMapper.selectPage(page,
                new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getUserId, userId)
                        .orderByDesc(ConversationMember::getJoinedAt));

        List<ConversationMember> members = memberPage.getRecords();
        IPage<ConversationVO> result = new Page<>(memberPage.getCurrent(), memberPage.getSize(), memberPage.getTotal());
        if (members == null || members.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        Set<Long> conversationIds = members.stream().map(ConversationMember::getConversationId).collect(Collectors.toSet());
        Map<Long, Conversation> conversationMap = conversationMapper.selectBatchIds(conversationIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Conversation::getId, Function.identity(), (a, b) -> a));

        Set<Long> spaceIds = conversationMap.values().stream()
                .map(Conversation::getSpaceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Space> spaceMap = spaceIds.isEmpty()
                ? Collections.emptyMap()
                : spaceMapper.selectBatchIds(spaceIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Space::getId, Function.identity(), (a, b) -> a));

        Map<Long, ConversationMember> memberMap = members.stream()
                .collect(Collectors.toMap(ConversationMember::getConversationId, Function.identity(), (a, b) -> a));

        Map<Long, UserVO> peerMap = loadDmPeerMap(conversationMap.values(), userId);

        List<ConversationVO> vos = conversationIds.stream()
                .map(conversationMap::get)
                .filter(Objects::nonNull)
                .map(c -> toConversationVO(c, memberMap.get(c.getId()), spaceMap, peerMap, userId))
                .sorted((a, b) -> {
                    var ta = a.getLastMessage() != null ? a.getLastMessage().getCreateTime() : a.getUpdateTime();
                    var tb = b.getLastMessage() != null ? b.getLastMessage().getCreateTime() : b.getUpdateTime();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                })
                .toList();
        result.setRecords(vos);
        return result;
    }

    @Override
    public ConversationVO getSpaceConversation(Long spaceId, Long userId) {
        spaceService.requireRoleAtLeast(spaceId, userId, SpaceRole.VIEWER);
        Conversation conversation = requireSpaceConversationEntity(spaceId);
        ConversationMember member = requireMember(conversation.getId(), userId);
        Map<Long, Space> spaceMap = Map.of(spaceId, spaceService.requireSpace(spaceId));
        return toConversationVO(conversation, member, spaceMap, Collections.emptyMap(), userId);
    }

    @Override
    @Transactional
    public ConversationVO openOrGetDm(Long userId, Long peerUserId) {
        ConversationLifecycleService.DmOpenResult result =
                conversationLifecycleService.openOrGetDm(userId, peerUserId);
        Conversation conversation = result.conversation();
        ConversationMember member = requireMember(conversation.getId(), userId);
        Map<Long, UserVO> peerMap = loadDmPeerMap(List.of(conversation), userId);
        ConversationVO vo = toConversationVO(conversation, member, Collections.emptyMap(), peerMap, userId);
        if (result.created()) {
            chatEventPublisher.publish(ChatEvent.conversationUpdated(
                    conversation.getId(), vo.getUnreadCount(), vo.getLastMessage(), userId));
            chatEventPublisher.publish(ChatEvent.conversationUpdated(
                    conversation.getId(), 0L, null, peerUserId));
        }
        return vo;
    }

    @Override
    public List<UserVO> listConversationMembers(Long conversationId, Long userId) {
        requireMember(conversationId, userId);
        List<Long> memberIds = conversationMemberMapper.selectUserIdsByConversationId(conversationId);
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        Map<Long, UserVO> map = loadUserVOMap(new HashSet<>(memberIds));
        return memberIds.stream().map(map::get).filter(Objects::nonNull).toList();
    }

    @Override
    public IPage<ChatMessageVO> pageMessages(Long conversationId, Long userId, PageRequest pageRequest) {
        requireMember(conversationId, userId);
        PageRequest req = pageRequest != null ? pageRequest : new PageRequest();
        Page<ChatMessage> page = new Page<>(req.getCurrent(), req.getPageSize());
        Page<ChatMessage> result = chatMessageMapper.selectPage(page, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getId));
        IPage<ChatMessageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toVoList(result.getRecords()));
        return voPage;
    }

    @Override
    public List<ChatMessageVO> listMessagesSince(Long conversationId, Long userId, Long sinceId, int limit) {
        requireMember(conversationId, userId);
        if (sinceId == null || sinceId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sinceId 无效");
        }
        int size = limit > 0 ? Math.min(limit, 200) : DEFAULT_SINCE_LIMIT;
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .gt(ChatMessage::getId, sinceId)
                .orderByAsc(ChatMessage::getId)
                .last("LIMIT " + size));
        return toVoList(messages);
    }

    @Override
    @Transactional
    public ChatMessageVO sendMessage(Long conversationId, ChatMessageAddRequest request, Long userId) {
        requireMember(conversationId, userId);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能超过 500 个字符");
        }
        sensitiveWordService.assertCleanOrBlock(conversationId, userId, ChatMessageType.TEXT, content);

        String clientMsgId = normalizeClientMsgId(request.getClientMsgId());
        ChatMessageVO existingVo = findExistingByClientMsgId(userId, clientMsgId);
        if (existingVo != null) {
            return existingVo;
        }

        Long replyToId = resolveReplyToId(conversationId, request.getReplyToId());
        List<Long> mentionIds = resolveMentionUserIds(conversationId, userId, request.getMentionUserIds());

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setMessageType(ChatMessageType.TEXT);
        message.setContent(content);
        message.setReplyToId(replyToId);
        message.setClientMsgId(clientMsgId);

        insertMessage(message, userId, clientMsgId);
        saveMentionsAndNotify(message, mentionIds, userId);
        return publishNewMessage(message, userId);
    }

    @Override
    @Transactional
    public ChatMessageVO sendImageMessage(Long conversationId, MultipartFile file, String caption, Long replyToId,
                                          String clientMsgIdRaw, List<Long> mentionUserIds, Long userId) {
        requireMember(conversationId, userId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型不支持，仅支持 jpg/png/gif/webp");
        }
        long size = file.getSize();
        if (size > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 10MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        String captionText = caption == null ? "" : caption.trim();
        if (captionText.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "配文不能超过 500 个字符");
        }
        if (StringUtils.hasText(captionText)) {
            sensitiveWordService.assertCleanOrBlock(conversationId, userId, ChatMessageType.IMAGE, captionText);
        }

        String clientMsgId = normalizeClientMsgId(clientMsgIdRaw);
        ChatMessageVO existingVo = findExistingByClientMsgId(userId, clientMsgId);
        if (existingVo != null) {
            return existingVo;
        }

        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取图片文件");
        }
        if (image == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不是有效的图片");
        }

        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0) {
            ext = originalName.substring(dot);
        }
        String key = generateChatImageKey(ext);
        String bucket = cosProperties.getBucket();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            metadata.setContentType(contentType);
            cosClient.putObject(bucket, key, file.getInputStream(), metadata);
        } catch (Exception e) {
            log.error("聊天图片上传 COS 失败, bucket={}, key={}", bucket, key, e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "图片上传失败，请稍后重试");
        }
        String url = cosProperties.getBaseUrl() + "/" + key;

        Long resolvedReplyToId = resolveReplyToId(conversationId, replyToId);
        List<Long> mentionIds = resolveMentionUserIds(conversationId, userId, mentionUserIds);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setMessageType(ChatMessageType.IMAGE);
        message.setContent(captionText);
        message.setMediaUrl(url);
        message.setMediaWidth(image.getWidth());
        message.setMediaHeight(image.getHeight());
        message.setMediaSize(size);
        message.setMediaContentType(contentType);
        message.setReplyToId(resolvedReplyToId);
        message.setClientMsgId(clientMsgId);

        insertMessage(message, userId, clientMsgId);
        saveMentionsAndNotify(message, mentionIds, userId);
        return publishNewMessage(message, userId);
    }

    @Override
    @Transactional
    public void deleteMessage(Long conversationId, Long messageId, Long operatorId) {
        if (messageId == null || messageId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 不能为空");
        }
        requireMember(conversationId, operatorId);
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getConversationId(), conversationId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不存在");
        }

        boolean isAuthor = Objects.equals(message.getSenderId(), operatorId);
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null && ConversationType.DM.equals(conversation.getType())) {
            if (!isAuthor) {
                throw new BusinessException(ErrorCode.NO_AUTH, "只能删除自己的消息");
            }
        } else {
            boolean isCreator = false;
            if (conversation != null && ConversationType.SPACE.equals(conversation.getType())
                    && conversation.getSpaceId() != null) {
                SpaceMember spaceMember = spaceService.requireMember(conversation.getSpaceId(), operatorId);
                isCreator = SpaceRole.CREATOR.equals(spaceMember.getRole());
            }
            if (!isAuthor && !isCreator) {
                throw new BusinessException(ErrorCode.NO_AUTH, "只能删除自己的消息或由创建者删除");
            }
        }

        int rows = chatMessageMapper.deleteById(messageId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "删除消息失败");
        }
        List<Long> targets = conversationMemberMapper.selectUserIdsByConversationId(conversationId);
        chatEventPublisher.publish(ChatEvent.messageDeleted(conversationId, messageId, targets));
    }

    @Override
    @Transactional
    public void markRead(Long conversationId, ChatReadRequest request, Long userId) {
        ConversationMember member = requireMember(conversationId, userId);
        if (request == null || request.getLastReadMessageId() == null || request.getLastReadMessageId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "lastReadMessageId 无效");
        }
        long current = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
        long next = request.getLastReadMessageId();
        if (next <= current) {
            return;
        }
        member.setLastReadMessageId(next);
        conversationMemberMapper.updateById(member);

        ChatMessage last = chatMessageMapper.selectById(next);
        ChatMessageVO lastVo = last != null && Objects.equals(last.getConversationId(), conversationId)
                ? toVoList(List.of(last)).getFirst()
                : null;
        chatEventPublisher.publish(ChatEvent.conversationUpdated(conversationId, 0L, lastVo, userId));
    }

    @Override
    public Long resolveSpaceConversationId(Long spaceId) {
        Conversation conversation = requireSpaceConversationEntity(spaceId);
        return conversation.getId();
    }

    private void insertMessage(ChatMessage message, Long userId, String clientMsgId) {
        try {
            int rows = chatMessageMapper.insert(message);
            if (rows <= 0 || message.getId() == null) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "发送消息失败");
            }
        } catch (DuplicateKeyException e) {
            ChatMessage existing = findMessageByClientMsgId(userId, clientMsgId);
            if (existing != null) {
                message.setId(existing.getId());
                return;
            }
            throw new BusinessException(ErrorCode.SERVER_ERROR, "发送消息失败");
        }
    }

    private ChatMessageVO publishNewMessage(ChatMessage message, Long userId) {
        // 若幂等命中已有消息，直接返回 VO
        ChatMessage persisted = chatMessageMapper.selectById(message.getId());
        if (persisted == null) {
            persisted = message;
        }
        ChatMessageVO vo = toVoList(List.of(persisted)).getFirst();
        Long conversationId = persisted.getConversationId();
        List<Long> targets = conversationMemberMapper.selectUserIdsByConversationId(conversationId);
        chatEventPublisher.publish(ChatEvent.messageNew(conversationId, vo, targets));

        for (Long targetId : targets) {
            if (Objects.equals(targetId, userId)) {
                continue;
            }
            ConversationMember member = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                    .eq(ConversationMember::getConversationId, conversationId)
                    .eq(ConversationMember::getUserId, targetId)
                    .last("LIMIT 1"));
            long unread = member == null ? 0
                    : chatMessageMapper.countUnread(conversationId,
                    member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId(),
                    targetId);
            chatEventPublisher.publish(ChatEvent.conversationUpdated(conversationId, unread, vo, targetId));
        }
        return vo;
    }

    private void saveMentionsAndNotify(ChatMessage message, List<Long> mentionIds, Long senderId) {
        if (mentionIds == null || mentionIds.isEmpty() || message.getId() == null) {
            return;
        }
        // 幂等重入时可能已写过 mention，先查再插
        Long existingCount = chatMessageMentionMapper.selectCount(new LambdaQueryWrapper<ChatMessageMention>()
                .eq(ChatMessageMention::getMessageId, message.getId()));
        if (existingCount != null && existingCount > 0) {
            return;
        }

        Conversation conversation = conversationMapper.selectById(message.getConversationId());
        Long spaceId = conversation != null && ConversationType.SPACE.equals(conversation.getType())
                ? conversation.getSpaceId() : null;
        String summary = ChatMessageType.IMAGE.equals(message.getMessageType())
                ? (StringUtils.hasText(message.getContent()) ? message.getContent() : "[图片]")
                : message.getContent();

        for (Long mentionUserId : mentionIds) {
            ChatMessageMention mention = new ChatMessageMention();
            mention.setMessageId(message.getId());
            mention.setUserId(mentionUserId);
            chatMessageMentionMapper.insert(mention);
            notificationService.create(
                    mentionUserId,
                    senderId,
                    NotificationType.CHAT_MENTION,
                    null,
                    null,
                    spaceId,
                    message.getConversationId(),
                    summary
            );
        }
    }

    private List<Long> resolveMentionUserIds(Long conversationId, Long senderId, List<Long> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long id : rawIds) {
            if (id != null && id > 0) {
                unique.add(id);
            }
        }
        if (unique.isEmpty()) {
            return List.of();
        }
        if (unique.size() > MAX_MENTIONS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单条消息最多 @ " + MAX_MENTIONS + " 人");
        }
        if (unique.contains(senderId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能 @ 自己");
        }
        Set<Long> memberIds = new HashSet<>(conversationMemberMapper.selectUserIdsByConversationId(conversationId));
        for (Long id : unique) {
            if (!memberIds.contains(id)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能 @ 本会话成员");
            }
        }
        return new ArrayList<>(unique);
    }

    private Long resolveReplyToId(Long conversationId, Long replyToId) {
        if (replyToId == null) {
            return null;
        }
        if (replyToId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复消息 ID 无效");
        }
        ChatMessage replyTo = chatMessageMapper.selectById(replyToId);
        if (replyTo == null || !Objects.equals(replyTo.getConversationId(), conversationId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被回复的消息不存在");
        }
        return replyTo.getId();
    }

    private String normalizeClientMsgId(String clientMsgId) {
        return StringUtils.hasText(clientMsgId) ? clientMsgId.trim() : null;
    }

    private ChatMessageVO findExistingByClientMsgId(Long userId, String clientMsgId) {
        ChatMessage existing = findMessageByClientMsgId(userId, clientMsgId);
        if (existing == null) {
            return null;
        }
        return toVoList(List.of(existing)).getFirst();
    }

    private ChatMessage findMessageByClientMsgId(Long userId, String clientMsgId) {
        if (clientMsgId == null) {
            return null;
        }
        return chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSenderId, userId)
                .eq(ChatMessage::getClientMsgId, clientMsgId)
                .last("LIMIT 1"));
    }

    private String generateChatImageKey(String ext) {
        LocalDate today = LocalDate.now();
        return String.format("chat/%d/%02d/%02d/%s%s",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                ext);
    }

    private ConversationVO toConversationVO(Conversation conversation,
                                            ConversationMember member,
                                            Map<Long, Space> spaceMap,
                                            Map<Long, UserVO> peerMap,
                                            Long userId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setType(conversation.getType());
        vo.setSpaceId(conversation.getSpaceId());
        vo.setUpdateTime(conversation.getUpdateTime());
        if (ConversationType.SPACE.equals(conversation.getType()) && conversation.getSpaceId() != null) {
            Space space = spaceMap.get(conversation.getSpaceId());
            if (space != null) {
                vo.setSpaceName(space.getName());
                vo.setTitle(space.getName());
            }
        } else if (ConversationType.DM.equals(conversation.getType())) {
            UserVO peer = peerMap != null ? peerMap.get(conversation.getId()) : null;
            vo.setPeer(peer);
            if (peer != null) {
                String name = StringUtils.hasText(peer.getUserName()) ? peer.getUserName() : peer.getUserAccount();
                vo.setTitle(name);
            }
        }
        long lastRead = member != null && member.getLastReadMessageId() != null
                ? member.getLastReadMessageId() : 0L;
        vo.setUnreadCount(chatMessageMapper.countUnread(conversation.getId(), lastRead, userId));

        ChatMessage last = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1"));
        if (last != null) {
            List<ChatMessageVO> list = toVoList(List.of(last));
            vo.setLastMessage(list.isEmpty() ? null : list.getFirst());
        }
        return vo;
    }

    private Map<Long, UserVO> loadDmPeerMap(java.util.Collection<Conversation> conversations, Long userId) {
        if (conversations == null || conversations.isEmpty() || userId == null) {
            return Collections.emptyMap();
        }
        List<Long> dmIds = conversations.stream()
                .filter(c -> c != null && ConversationType.DM.equals(c.getType()))
                .map(Conversation::getId)
                .filter(Objects::nonNull)
                .toList();
        if (dmIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ConversationMember> dmMembers = conversationMemberMapper.selectList(
                new LambdaQueryWrapper<ConversationMember>()
                        .in(ConversationMember::getConversationId, dmIds));
        Map<Long, Long> conversationPeerId = new HashMap<>();
        for (ConversationMember m : dmMembers) {
            if (m.getUserId() != null && !Objects.equals(m.getUserId(), userId)) {
                conversationPeerId.put(m.getConversationId(), m.getUserId());
            }
        }
        if (conversationPeerId.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, UserVO> users = loadUserVOMap(new HashSet<>(conversationPeerId.values()));
        Map<Long, UserVO> result = new HashMap<>();
        conversationPeerId.forEach((cid, peerId) -> {
            UserVO peer = users.get(peerId);
            if (peer != null) {
                result.put(cid, peer);
            }
        });
        return result;
    }

    private ConversationMember requireMember(Long conversationId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");
        }
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在");
        }
        ConversationMember member = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "不是该会话成员");
        }
        if (ConversationType.SPACE.equals(conversation.getType()) && conversation.getSpaceId() != null) {
            spaceService.requireRoleAtLeast(conversation.getSpaceId(), userId, SpaceRole.VIEWER);
        }
        return member;
    }

    private Conversation requireSpaceConversationEntity(Long spaceId) {
        Conversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getType, ConversationType.SPACE)
                .eq(Conversation::getSpaceId, spaceId)
                .last("LIMIT 1"));
        if (conversation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在");
        }
        return conversation;
    }

    private List<ChatMessageVO> toVoList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> replyToIds = new HashSet<>();
        List<Long> messageIds = new ArrayList<>();
        for (ChatMessage message : messages) {
            messageIds.add(message.getId());
            if (message.getSenderId() != null) {
                userIds.add(message.getSenderId());
            }
            if (message.getReplyToId() != null) {
                replyToIds.add(message.getReplyToId());
            }
        }
        Map<Long, ChatMessage> replyMap = replyToIds.isEmpty()
                ? Collections.emptyMap()
                : chatMessageMapper.selectBatchIdsIncludeDeleted(replyToIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, Function.identity(), (a, b) -> a));
        for (ChatMessage reply : replyMap.values()) {
            if (reply.getSenderId() != null) {
                userIds.add(reply.getSenderId());
            }
        }

        Map<Long, List<Long>> mentionUserIdsByMessage = new HashMap<>();
        if (!messageIds.isEmpty()) {
            List<ChatMessageMention> mentions = chatMessageMentionMapper.selectByMessageIds(messageIds);
            for (ChatMessageMention mention : mentions) {
                mentionUserIdsByMessage
                        .computeIfAbsent(mention.getMessageId(), k -> new ArrayList<>())
                        .add(mention.getUserId());
                if (mention.getUserId() != null) {
                    userIds.add(mention.getUserId());
                }
            }
        }

        Map<Long, UserVO> userVOMap = loadUserVOMap(userIds);
        return messages.stream().map(message -> {
            ChatMessageVO vo = ChatMessageConverter.toVO(message);
            vo.setSender(userVOMap.get(message.getSenderId()));
            if (message.getReplyToId() != null) {
                vo.setReplyTo(buildReplyToVO(replyMap.get(message.getReplyToId()), userVOMap));
            }
            List<Long> mentionIds = mentionUserIdsByMessage.getOrDefault(message.getId(), List.of());
            if (!mentionIds.isEmpty()) {
                vo.setMentions(mentionIds.stream()
                        .map(userVOMap::get)
                        .filter(Objects::nonNull)
                        .toList());
            } else {
                vo.setMentions(List.of());
            }
            return vo;
        }).toList();
    }

    private ChatMessageReplyToVO buildReplyToVO(ChatMessage reply, Map<Long, UserVO> userVOMap) {
        ChatMessageReplyToVO replyToVO = new ChatMessageReplyToVO();
        if (reply == null) {
            replyToVO.setDeleted(true);
            return replyToVO;
        }
        replyToVO.setId(reply.getId());
        boolean deleted = Objects.equals(reply.getIsDelete(), 1);
        replyToVO.setDeleted(deleted);
        replyToVO.setMessageType(StringUtils.hasText(reply.getMessageType())
                ? reply.getMessageType() : ChatMessageType.TEXT);
        replyToVO.setContent(deleted ? null : reply.getContent());
        replyToVO.setSender(userVOMap.get(reply.getSenderId()));
        return replyToVO;
    }

    private Map<Long, UserVO> loadUserVOMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, UserConverter::toVO, (a, b) -> a));
    }
}
