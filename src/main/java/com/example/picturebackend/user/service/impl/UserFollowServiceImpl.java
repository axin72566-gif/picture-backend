package com.example.picturebackend.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.notification.constant.NotificationType;
import com.example.picturebackend.notification.service.NotificationService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.entity.UserFollow;
import com.example.picturebackend.user.mapper.UserFollowMapper;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
import com.example.picturebackend.user.service.UserFollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowMapper userFollowMapper;

    private final UserMapper userMapper;

    private final NotificationService notificationService;

    public UserFollowServiceImpl(UserFollowMapper userFollowMapper,
                                 UserMapper userMapper,
                                 NotificationService notificationService) {
        this.userFollowMapper = userFollowMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void follow(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能关注自己");
        }

        User followed = userMapper.selectById(followedId);
        if (followed == null || followed.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowedId, followedId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已关注该用户");
        }

        // 兼容历史软删除残留：唯一索引仍占用时先恢复，避免再次关注失败
        int restored = userFollowMapper.restoreSoftDeleted(followerId, followedId);
        if (restored > 0) {
            notificationService.create(followedId, followerId, NotificationType.FOLLOW, null, null, null);
            return;
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(followerId);
        userFollow.setFollowedId(followedId);
        int rows = userFollowMapper.insert(userFollow);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "关注失败");
        }
        notificationService.create(followedId, followerId, NotificationType.FOLLOW, null, null, null);
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followedId) {
        // 物理删除，确保库中不再保留关注关系行（软删除会撞唯一索引且行仍可见）
        int rows = userFollowMapper.deletePhysically(followerId, followedId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未关注该用户");
        }
    }

    @Override
    public boolean isFollowed(Long followerId, Long followedId) {
        if (followerId == null) {
            return false;
        }
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowedId, followedId)
                .eq(UserFollow::getIsDelete, 0));
        return count != null && count > 0;
    }

    @Override
    public IPage<UserVO> pageFollowers(Long userId, PageRequest request) {
        Page<UserFollow> page = new Page<>(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowedId, userId)
                .eq(UserFollow::getIsDelete, 0)
                .orderByDesc(UserFollow::getCreateTime);
        Page<UserFollow> result = userFollowMapper.selectPage(page, wrapper);

        List<Long> userIds = result.getRecords().stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            IPage<UserVO> emptyPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserVO> voList = userIds.stream()
                .map(id -> userMap.get(id))
                .filter(Objects::nonNull)
                .map(UserConverter::toVO)
                .collect(Collectors.toList());

        IPage<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<UserVO> pageFollowing(Long userId, PageRequest request) {
        Page<UserFollow> page = new Page<>(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getIsDelete, 0)
                .orderByDesc(UserFollow::getCreateTime);
        Page<UserFollow> result = userFollowMapper.selectPage(page, wrapper);

        List<Long> userIds = result.getRecords().stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            IPage<UserVO> emptyPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserVO> voList = userIds.stream()
                .map(id -> userMap.get(id))
                .filter(Objects::nonNull)
                .map(UserConverter::toVO)
                .collect(Collectors.toList());

        IPage<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public long countFollowers(Long userId) {
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowedId, userId)
                .eq(UserFollow::getIsDelete, 0));
        return count != null ? count : 0;
    }

    @Override
    public long countFollowing(Long userId) {
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getIsDelete, 0));
        return count != null ? count : 0;
    }
}
