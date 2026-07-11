package com.example.picturebackend.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.user.model.vo.UserVO;

public interface UserFollowService {

    void follow(Long followerId, Long followedId);

    void unfollow(Long followerId, Long followedId);

    boolean isFollowed(Long followerId, Long followedId);

    IPage<UserVO> pageFollowers(Long userId, PageRequest request);

    IPage<UserVO> pageFollowing(Long userId, PageRequest request);

    long countFollowers(Long userId);

    long countFollowing(Long userId);
}
