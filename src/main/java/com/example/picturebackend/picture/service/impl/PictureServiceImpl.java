package com.example.picturebackend.picture.service.impl;

import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.config.CosProperties;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.picture.entity.Picture;
import com.example.picturebackend.picture.mapper.PictureMapper;
import com.example.picturebackend.picture.model.converter.PictureConverter;
import com.example.picturebackend.picture.model.vo.PictureVO;
import com.example.picturebackend.picture.service.PictureService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PictureServiceImpl implements PictureService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private final PictureMapper pictureMapper;

    private final COSClient cosClient;

    private final CosProperties cosProperties;

    public PictureServiceImpl(PictureMapper pictureMapper, COSClient cosClient, CosProperties cosProperties) {
        this.pictureMapper = pictureMapper;
        this.cosClient = cosClient;
        this.cosProperties = cosProperties;
    }

    @Override
    public PictureVO uploadPicture(MultipartFile file, Long userId) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型不支持，仅支持 jpg/png/gif/webp");
        }

        long size = file.getSize();
        if (size > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 10MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取图片文件");
        }
        if (image == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不是有效的图片");
        }
        int width = image.getWidth();
        int height = image.getHeight();

        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0) {
            ext = originalName.substring(dot);
        }

        String key = generateKey(ext);
        String bucket = cosProperties.getBucket();

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            metadata.setContentType(contentType);
            cosClient.putObject(bucket, key, file.getInputStream(), metadata);
        } catch (Exception e) {
            log.error("上传 COS 失败, bucket={}, key={}", bucket, key, e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "图片上传失败，请稍后重试");
        }

        String url = cosProperties.getBaseUrl() + "/" + key;

        Picture picture = new Picture();
        picture.setUrl(url);
        picture.setName(originalName);
        picture.setSize(size);
        picture.setWidth(width);
        picture.setHeight(height);
        picture.setContentType(contentType);
        picture.setFormat(ext.isEmpty() ? null : ext.substring(1));
        picture.setUserId(userId);
        pictureMapper.insert(picture);

        log.info("图片上传成功 id={}, url={}, userId={}", picture.getId(), url, userId);
        return PictureConverter.toVO(picture);
    }

    private String generateKey(String ext) {
        LocalDate today = LocalDate.now();
        return String.format("%d/%02d/%02d/%s%s",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                ext);
    }
}
