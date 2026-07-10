package com.example.picturebackend.picture.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.picture.model.dto.PictureQueryRequest;
import com.example.picturebackend.picture.model.dto.PictureUpdateRequest;
import com.example.picturebackend.picture.model.vo.PictureVO;
import com.example.picturebackend.picture.service.PictureService;
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

    public PictureController(PictureService pictureService) {
        this.pictureService = pictureService;
    }

    @PostMapping("/upload")
    public BaseResponse<PictureVO> upload(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        PictureVO vo = pictureService.uploadPicture(file, userId);
        return ResultUtils.success(vo);
    }

    @GetMapping("/page")
    public BaseResponse<IPage<PictureVO>> pagePictures(PictureQueryRequest request) {
        IPage<PictureVO> page = pictureService.pagePictures(request);
        return ResultUtils.success(page);
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
}
