package com.example.picturebackend.user.model.converter;

import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.model.vo.UserVO;

import java.time.LocalDateTime;

public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserProfile(user.getUserProfile());
        vo.setUserRole(user.getUserRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setVipExpireTime(user.getVipExpireTime());
        vo.setVipActive(user.getVipExpireTime() != null && user.getVipExpireTime().isAfter(LocalDateTime.now()));
        return vo;
    }
}
