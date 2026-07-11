package com.example.picturebackend.user.service;

import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.model.dto.UserLoginRequest;
import com.example.picturebackend.user.model.dto.UserRegisterRequest;
import com.example.picturebackend.user.model.dto.UserUpdateRequest;
import com.example.picturebackend.user.model.vo.LoginUserVO;
import com.example.picturebackend.user.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    long userRegister(UserRegisterRequest request);

    LoginUserVO userLogin(UserLoginRequest request);

    boolean userLogout(HttpServletRequest request);

    UserVO getLoginUser(HttpServletRequest request);

    User getById(Long id);

    UserVO getUserVO(Long id);

    UserVO updateUser(UserUpdateRequest updateRequest, Long userId);

    String uploadAvatar(MultipartFile file, Long userId);
}