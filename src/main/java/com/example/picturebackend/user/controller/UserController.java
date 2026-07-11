package com.example.picturebackend.user.controller;

import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.user.model.dto.UserLoginRequest;
import com.example.picturebackend.user.model.dto.UserRegisterRequest;
import com.example.picturebackend.user.model.dto.UserUpdateRequest;
import com.example.picturebackend.user.model.vo.LoginUserVO;
import com.example.picturebackend.user.model.vo.UserVO;
import com.example.picturebackend.user.service.UserFollowService;
import com.example.picturebackend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    private final UserFollowService userFollowService;

    public UserController(UserService userService, UserFollowService userFollowService) {
        this.userService = userService;
        this.userFollowService = userFollowService;
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
        enrichFollowCounts(vo);
        return ResultUtils.success(vo);
    }

    @GetMapping("/{id}")
    public BaseResponse<UserVO> getUserById(@PathVariable Long id) {
        UserVO vo = userService.getUserVO(id);
        enrichFollowCounts(vo);
        return ResultUtils.success(vo);
    }

    @PutMapping("/update")
    public BaseResponse<UserVO> updateUser(@RequestBody UserUpdateRequest request,
                                           HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        UserVO vo = userService.updateUser(request, userId);
        enrichFollowCounts(vo);
        return ResultUtils.success(vo);
    }

    @PostMapping("/avatar/upload")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                              HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String url = userService.uploadAvatar(file, userId);
        return ResultUtils.success(url);
    }

    @PostMapping("/follow/{followedId}")
    public BaseResponse<Void> follow(@PathVariable Long followedId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        userFollowService.follow(userId, followedId);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/follow/{followedId}")
    public BaseResponse<Void> unfollow(@PathVariable Long followedId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        userFollowService.unfollow(userId, followedId);
        return ResultUtils.success(null);
    }

    @GetMapping("/{id}/followers")
    public BaseResponse<IPage<UserVO>> getFollowers(@PathVariable Long id, PageRequest pageRequest) {
        IPage<UserVO> page = userFollowService.pageFollowers(id, pageRequest);
        return ResultUtils.success(page);
    }

    @GetMapping("/{id}/following")
    public BaseResponse<IPage<UserVO>> getFollowing(@PathVariable Long id, PageRequest pageRequest) {
        IPage<UserVO> page = userFollowService.pageFollowing(id, pageRequest);
        return ResultUtils.success(page);
    }

    @GetMapping("/{id}/follow/status")
    public BaseResponse<Boolean> getFollowStatus(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        boolean followed = userFollowService.isFollowed(userId, id);
        return ResultUtils.success(followed);
    }

    private void enrichFollowCounts(UserVO vo) {
        if (vo != null) {
            vo.setFollowerCount(userFollowService.countFollowers(vo.getId()));
            vo.setFollowingCount(userFollowService.countFollowing(vo.getId()));
        }
    }
}