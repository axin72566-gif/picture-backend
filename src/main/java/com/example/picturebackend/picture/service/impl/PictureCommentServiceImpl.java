package com.example.picturebackend.picture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.picture.entity.Picture;
import com.example.picturebackend.picture.entity.PictureComment;
import com.example.picturebackend.picture.mapper.PictureCommentMapper;
import com.example.picturebackend.picture.mapper.PictureMapper;
import com.example.picturebackend.picture.model.converter.PictureCommentConverter;
import com.example.picturebackend.picture.model.dto.PictureCommentAddRequest;
import com.example.picturebackend.picture.model.dto.PictureCommentQueryRequest;
import com.example.picturebackend.picture.model.vo.PictureCommentVO;
import com.example.picturebackend.picture.service.PictureCommentService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PictureCommentServiceImpl implements PictureCommentService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final PictureCommentMapper pictureCommentMapper;

    private final PictureMapper pictureMapper;

    private final UserMapper userMapper;

    public PictureCommentServiceImpl(PictureCommentMapper pictureCommentMapper,
                                     PictureMapper pictureMapper,
                                     UserMapper userMapper) {
        this.pictureCommentMapper = pictureCommentMapper;
        this.pictureMapper = pictureMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public PictureCommentVO addComment(Long pictureId, PictureCommentAddRequest request, Long userId) {
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 ID 不能为空");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不能超过 500 个字符");
        }

        Picture picture = pictureMapper.selectById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }

        PictureComment comment = new PictureComment();
        comment.setPictureId(pictureId);
        comment.setUserId(userId);
        comment.setContent(content);

        Long parentId = request.getParentId();
        if (parentId == null) {
            comment.setParentId(null);
            comment.setRootId(null);
        } else {
            PictureComment parent = pictureCommentMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "被回复的评论不存在");
            }
            if (!Objects.equals(parent.getPictureId(), pictureId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能回复其他图片的评论");
            }
            Long rootId = parent.getRootId() != null ? parent.getRootId() : parent.getId();
            comment.setParentId(parent.getId());
            comment.setRootId(rootId);
        }

        int rows = pictureCommentMapper.insert(comment);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "发表评论失败");
        }

        PictureCommentVO vo = PictureCommentConverter.toVO(comment);
        vo.setReplyCount(0L);
        User user = userMapper.selectById(userId);
        vo.setUser(UserConverter.toVO(user));
        return vo;
    }

    @Override
    public IPage<PictureCommentVO> pageRootComments(Long pictureId, PictureCommentQueryRequest request) {
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 ID 不能为空");
        }
        Picture picture = pictureMapper.selectById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }

        PictureCommentQueryRequest query = request != null ? request : new PictureCommentQueryRequest();
        Page<PictureComment> page = new Page<>(query.getCurrent(), query.getPageSize());
        LambdaQueryWrapper<PictureComment> wrapper = new LambdaQueryWrapper<PictureComment>()
                .eq(PictureComment::getPictureId, pictureId)
                .isNull(PictureComment::getRootId)
                .eq(PictureComment::getIsDelete, 0)
                .orderByDesc(PictureComment::getCreateTime);
        Page<PictureComment> result = pictureCommentMapper.selectPage(page, wrapper);
        return toVoPage(result, true);
    }

    @Override
    public IPage<PictureCommentVO> pageReplies(Long rootId, PictureCommentQueryRequest request) {
        if (rootId == null || rootId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "根评论 ID 不能为空");
        }
        PictureComment root = pictureCommentMapper.selectById(rootId);
        if (root == null || root.getRootId() != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "根评论不存在");
        }

        PictureCommentQueryRequest query = request != null ? request : new PictureCommentQueryRequest();
        Page<PictureComment> page = new Page<>(query.getCurrent(), query.getPageSize());
        LambdaQueryWrapper<PictureComment> wrapper = new LambdaQueryWrapper<PictureComment>()
                .eq(PictureComment::getRootId, rootId)
                .eq(PictureComment::getIsDelete, 0)
                .orderByAsc(PictureComment::getCreateTime);
        Page<PictureComment> result = pictureCommentMapper.selectPage(page, wrapper);
        return toVoPage(result, false);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论 ID 不能为空");
        }
        PictureComment comment = pictureCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论不存在");
        }

        Picture picture = pictureMapper.selectById(comment.getPictureId());
        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }

        boolean isAuthor = Objects.equals(comment.getUserId(), userId);
        boolean isPictureOwner = Objects.equals(picture.getUserId(), userId);
        if (!isAuthor && !isPictureOwner) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能删除自己的评论或自己图片下的评论");
        }

        pictureCommentMapper.deleteById(commentId);

        // 删除根评论时级联软删其下所有回复
        if (comment.getRootId() == null) {
            pictureCommentMapper.delete(new LambdaQueryWrapper<PictureComment>()
                    .eq(PictureComment::getRootId, commentId));
        }
    }

    private IPage<PictureCommentVO> toVoPage(Page<PictureComment> result, boolean withReplyCount) {
        List<PictureComment> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            IPage<PictureCommentVO> emptyPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Set<Long> userIds = records.stream()
                .map(PictureComment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, UserConverter::toVO, (a, b) -> a));

        Map<Long, Long> replyCountMap = Collections.emptyMap();
        if (withReplyCount) {
            List<Long> rootIds = records.stream().map(PictureComment::getId).toList();
            replyCountMap = loadReplyCounts(rootIds);
        }

        Map<Long, Long> finalReplyCountMap = replyCountMap;
        List<PictureCommentVO> voList = records.stream().map(comment -> {
            PictureCommentVO vo = PictureCommentConverter.toVO(comment);
            vo.setUser(userVOMap.get(comment.getUserId()));
            if (withReplyCount) {
                vo.setReplyCount(finalReplyCountMap.getOrDefault(comment.getId(), 0L));
            } else {
                vo.setReplyCount(null);
            }
            return vo;
        }).toList();

        IPage<PictureCommentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private Map<Long, Long> loadReplyCounts(List<Long> rootIds) {
        if (rootIds == null || rootIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PictureComment> replies = pictureCommentMapper.selectList(new LambdaQueryWrapper<PictureComment>()
                .in(PictureComment::getRootId, rootIds)
                .eq(PictureComment::getIsDelete, 0)
                .select(PictureComment::getRootId));
        return replies.stream()
                .filter(c -> c.getRootId() != null)
                .collect(Collectors.groupingBy(PictureComment::getRootId, Collectors.counting()));
    }
}
