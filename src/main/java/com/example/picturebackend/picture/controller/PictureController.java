package com.example.picturebackend.picture.controller;

import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.picture.model.vo.PictureVO;
import com.example.picturebackend.picture.service.PictureService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
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
}
