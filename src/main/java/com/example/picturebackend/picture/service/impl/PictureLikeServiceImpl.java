package com.example.picturebackend.picture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.notification.constant.NotificationType;
import com.example.picturebackend.notification.service.NotificationService;
import com.example.picturebackend.picture.entity.Picture;
import com.example.picturebackend.picture.entity.PictureLike;
import com.example.picturebackend.picture.mapper.PictureLikeMapper;
import com.example.picturebackend.picture.mapper.PictureMapper;
import com.example.picturebackend.picture.service.PictureLikeService;
import com.example.picturebackend.space.constant.SpaceRole;
import com.example.picturebackend.space.service.SpaceService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PictureLikeServiceImpl implements PictureLikeService {

    private final PictureLikeMapper pictureLikeMapper;

    private final PictureMapper pictureMapper;

    private final UserMapper userMapper;

    private final NotificationService notificationService;

    private final SpaceService spaceService;

    public PictureLikeServiceImpl(PictureLikeMapper pictureLikeMapper,
                                  PictureMapper pictureMapper,
                                  UserMapper userMapper,
                                  NotificationService notificationService,
                                  SpaceService spaceService) {
        this.pictureLikeMapper = pictureLikeMapper;
        this.pictureMapper = pictureMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.spaceService = spaceService;
    }

    @Override
    @Transactional
    public void like(Long userId, Long pictureId) {
        Picture picture = requireAccessiblePicture(pictureId, userId);
        if (Objects.equals(userId, picture.getUserId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能给自己的图片点赞");
        }

        Long count = pictureLikeMapper.selectCount(new LambdaQueryWrapper<PictureLike>()
                .eq(PictureLike::getUserId, userId)
                .eq(PictureLike::getPictureId, pictureId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已点赞该图片");
        }

        int restored = pictureLikeMapper.restoreSoftDeleted(userId, pictureId);
        if (restored > 0) {
            notificationService.create(picture.getUserId(), userId, NotificationType.LIKE, pictureId, null, null);
            return;
        }

        PictureLike pictureLike = new PictureLike();
        pictureLike.setUserId(userId);
        pictureLike.setPictureId(pictureId);
        int rows = pictureLikeMapper.insert(pictureLike);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "点赞失败");
        }
        notificationService.create(picture.getUserId(), userId, NotificationType.LIKE, pictureId, null, null);
    }

    @Override
    @Transactional
    public void unlike(Long userId, Long pictureId) {
        requireAccessiblePicture(pictureId, userId);
        int rows = pictureLikeMapper.deletePhysically(userId, pictureId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未点赞该图片");
        }
    }

    @Override
    public boolean isLiked(Long userId, Long pictureId) {
        if (userId == null || pictureId == null) {
            return false;
        }
        Picture picture = pictureMapper.selectById(pictureId);
        if (picture == null || Objects.equals(picture.getIsDelete(), 1)) {
            return false;
        }
        if (picture.getSpaceId() != null) {
            spaceService.requireRoleAtLeast(picture.getSpaceId(), userId, SpaceRole.VIEWER);
        }
        Long count = pictureLikeMapper.selectCount(new LambdaQueryWrapper<PictureLike>()
                .eq(PictureLike::getUserId, userId)
                .eq(PictureLike::getPictureId, pictureId)
                .eq(PictureLike::getIsDelete, 0));
        return count != null && count > 0;
    }

    @Override
    public long countByPictureId(Long pictureId) {
        if (pictureId == null) {
            return 0;
        }
        Long count = pictureLikeMapper.selectCount(new LambdaQueryWrapper<PictureLike>()
                .eq(PictureLike::getPictureId, pictureId)
                .eq(PictureLike::getIsDelete, 0));
        return count != null ? count : 0;
    }

    @Override
    public Map<Long, Long> countByPictureIds(Collection<Long> pictureIds) {
        if (CollectionUtils.isEmpty(pictureIds)) {
            return Collections.emptyMap();
        }
        List<PictureLike> likes = pictureLikeMapper.selectList(new LambdaQueryWrapper<PictureLike>()
                .in(PictureLike::getPictureId, pictureIds)
                .eq(PictureLike::getIsDelete, 0)
                .select(PictureLike::getPictureId));
        Map<Long, Long> result = new HashMap<>();
        for (PictureLike like : likes) {
            result.merge(like.getPictureId(), 1L, Long::sum);
        }
        return result;
    }

    @Override
    public Set<Long> findLikedPictureIds(Long userId, Collection<Long> pictureIds) {
        if (userId == null || CollectionUtils.isEmpty(pictureIds)) {
            return Collections.emptySet();
        }
        List<PictureLike> likes = pictureLikeMapper.selectList(new LambdaQueryWrapper<PictureLike>()
                .eq(PictureLike::getUserId, userId)
                .in(PictureLike::getPictureId, pictureIds)
                .eq(PictureLike::getIsDelete, 0)
                .select(PictureLike::getPictureId));
        Set<Long> result = new HashSet<>();
        for (PictureLike like : likes) {
            result.add(like.getPictureId());
        }
        return result;
    }

    @Override
    public IPage<UserVO> pageLikers(Long pictureId, PageRequest request, Long currentUserId) {
        requireAccessiblePicture(pictureId, currentUserId);

        Page<PictureLike> page = new Page<>(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<PictureLike> wrapper = new LambdaQueryWrapper<PictureLike>()
                .eq(PictureLike::getPictureId, pictureId)
                .eq(PictureLike::getIsDelete, 0)
                .orderByDesc(PictureLike::getCreateTime);
        Page<PictureLike> result = pictureLikeMapper.selectPage(page, wrapper);

        List<Long> userIds = result.getRecords().stream()
                .map(PictureLike::getUserId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            IPage<UserVO> emptyPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserVO> voList = userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(UserConverter::toVO)
                .collect(Collectors.toList());

        IPage<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 个人图任意可读；空间图需 VIEWER+（未登录抛 NOT_LOGIN）。
     */
    private Picture requireAccessiblePicture(Long pictureId, Long userId) {
        Picture picture = pictureMapper.selectById(pictureId);
        if (picture == null || Objects.equals(picture.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        if (picture.getSpaceId() != null) {
            spaceService.requireRoleAtLeast(picture.getSpaceId(), userId, SpaceRole.VIEWER);
        }
        return picture;
    }
}
