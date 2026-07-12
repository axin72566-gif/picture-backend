package com.example.picturebackend.picture.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.picture.model.dto.PictureQueryRequest;
import com.example.picturebackend.picture.model.dto.PictureUpdateRequest;
import com.example.picturebackend.picture.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

public interface PictureService {

    /**
     * 上传图片；spaceId 为空则为个人图，非空则为空间图（需 EDITOR+）。
     */
    PictureVO uploadPicture(MultipartFile file, Long userId, Long spaceId);

    IPage<PictureVO> pagePictures(PictureQueryRequest request, Long currentUserId);

    IPage<PictureVO> pageMyPictures(PictureQueryRequest request, Long userId);

    /**
     * 空间图片分页（需 VIEWER+）。
     */
    IPage<PictureVO> pageSpacePictures(Long spaceId, PictureQueryRequest request, Long userId);

    PictureVO updatePicture(PictureUpdateRequest request, Long userId);

    void deletePicture(Long id, Long userId);

    PictureVO getPictureById(Long id, Long currentUserId);
}
