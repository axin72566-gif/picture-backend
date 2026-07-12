package com.example.picturebackend.picture.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.user.model.vo.UserVO;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface PictureLikeService {

    void like(Long userId, Long pictureId);

    void unlike(Long userId, Long pictureId);

    boolean isLiked(Long userId, Long pictureId);

    long countByPictureId(Long pictureId);

    /**
     * 批量统计图片点赞数；未出现的 pictureId 视为 0。
     */
    Map<Long, Long> countByPictureIds(Collection<Long> pictureIds);

    /**
     * 返回 currentUserId 在给定图片集合中已点赞的 pictureId。
     */
    Set<Long> findLikedPictureIds(Long userId, Collection<Long> pictureIds);

    IPage<UserVO> pageLikers(Long pictureId, PageRequest request);
}
