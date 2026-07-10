package com.example.picturebackend.picture.service;

import com.example.picturebackend.picture.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

public interface PictureService {

    PictureVO uploadPicture(MultipartFile file, Long userId);
}
