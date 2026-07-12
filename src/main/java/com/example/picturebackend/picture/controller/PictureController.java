package com.example.picturebackend.picture.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.picture.model.dto.PictureQueryRequest;
import com.example.picturebackend.picture.model.dto.PictureUpdateRequest;
import com.example.picturebackend.picture.model.vo.PictureVO;
import com.example.picturebackend.picture.service.PictureLikeService;
import com.example.picturebackend.picture.service.PictureService;
import com.example.picturebackend.user.model.vo.UserVO;
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

@RestController
@RequestMapping("/api/picture")
public class PictureController {

    private final PictureService pictureService;

    private final PictureLikeService pictureLikeService;

    public PictureController(PictureService pictureService, PictureLikeService pictureLikeService) {
        this.pictureService = pictureService;
        this.pictureLikeService = pictureLikeService;
    }

    @PostMapping("/upload")
    public BaseResponse<PictureVO> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "spaceId", required = false) Long spaceId,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        PictureVO vo = pictureService.uploadPicture(file, userId, spaceId);
        return ResultUtils.success(vo);
    }

    @GetMapping("/page")
    public BaseResponse<IPage<PictureVO>> pagePictures(PictureQueryRequest request,
                                                       HttpServletRequest httpRequest) {
        Long currentUserId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        IPage<PictureVO> page = pictureService.pagePictures(request, currentUserId);
        return ResultUtils.success(page);
    }

    @GetMapping("/{id:\\d+}")
    public BaseResponse<PictureVO> getPictureById(@PathVariable Long id,
                                                  HttpServletRequest httpRequest) {
        Long currentUserId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        PictureVO vo = pictureService.getPictureById(id, currentUserId);
        return ResultUtils.success(vo);
    }

    @GetMapping("/my/page")
    public BaseResponse<IPage<PictureVO>> pageMyPictures(PictureQueryRequest request,
                                                         HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        IPage<PictureVO> page = pictureService.pageMyPictures(request, userId);
        return ResultUtils.success(page);
    }

    @PutMapping("/update")
    public BaseResponse<PictureVO> updatePicture(@RequestBody PictureUpdateRequest request,
                                                 HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        PictureVO vo = pictureService.updatePicture(request, userId);
        return ResultUtils.success(vo);
    }

    @DeleteMapping("/delete/{id}")
    public BaseResponse<Void> deletePicture(@PathVariable Long id,
                                            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        pictureService.deletePicture(id, userId);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id:\\d+}/like")
    public BaseResponse<Void> like(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        pictureLikeService.like(userId, id);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id:\\d+}/like")
    public BaseResponse<Void> unlike(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        pictureLikeService.unlike(userId, id);
        return ResultUtils.success(null);
    }

    @GetMapping("/{id:\\d+}/like/status")
    public BaseResponse<Boolean> getLikeStatus(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        boolean liked = pictureLikeService.isLiked(userId, id);
        return ResultUtils.success(liked);
    }

    @GetMapping("/{id:\\d+}/likes")
    public BaseResponse<IPage<UserVO>> pageLikers(@PathVariable Long id,
                                                  PageRequest pageRequest,
                                                  HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        IPage<UserVO> page = pictureLikeService.pageLikers(id, pageRequest, currentUserId);
        return ResultUtils.success(page);
    }
}
