package com.example.picturebackend.user.controller;

import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.user.model.dto.UserLoginRequest;
import com.example.picturebackend.user.model.dto.UserRegisterRequest;
import com.example.picturebackend.user.model.vo.LoginUserVO;
import com.example.picturebackend.user.model.vo.UserVO;
import com.example.picturebackend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest request) {
        long userId = userService.userRegister(request);
        return ResultUtils.success(userId);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest request) {
        LoginUserVO vo = userService.userLogin(request);
        return ResultUtils.success(vo);
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        boolean ok = userService.userLogout(request);
        return ResultUtils.success(ok);
    }

    @GetMapping("/current")
    public BaseResponse<UserVO> getCurrentUser(HttpServletRequest request) {
        UserVO vo = userService.getLoginUser(request);
        return ResultUtils.success(vo);
    }
}