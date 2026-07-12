package com.example.picturebackend.picture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.config.CosProperties;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.picture.entity.Picture;
import com.example.picturebackend.picture.mapper.PictureMapper;
import com.example.picturebackend.picture.model.converter.PictureConverter;
import com.example.picturebackend.picture.model.dto.PictureQueryRequest;
import com.example.picturebackend.picture.model.dto.PictureUpdateRequest;
import com.example.picturebackend.picture.model.vo.PictureVO;
import com.example.picturebackend.picture.service.PictureLikeService;
import com.example.picturebackend.picture.service.PictureService;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.vo.UserVO;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PictureServiceImpl implements PictureService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private static final Map<String, SFunction<Picture, ?>> SORT_FIELD_MAP = Map.of(
            "name", Picture::getName,
            "size", Picture::getSize,
            "createTime", Picture::getCreateTime
    );

    private final PictureMapper pictureMapper;

    private final UserMapper userMapper;

    private final PictureLikeService pictureLikeService;

    private final COSClient cosClient;

    private final CosProperties cosProperties;

    public PictureServiceImpl(PictureMapper pictureMapper,
                              UserMapper userMapper,
                              PictureLikeService pictureLikeService,
                              COSClient cosClient,
                              CosProperties cosProperties) {
        this.pictureMapper = pictureMapper;
        this.userMapper = userMapper;
        this.pictureLikeService = pictureLikeService;
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
        PictureVO vo = PictureConverter.toVO(picture);
        User user = userMapper.selectById(userId);
        vo.setUser(UserConverter.toVO(user));
        enrichLikeFields(List.of(vo), userId);
        return vo;
    }

    @Override
    public IPage<PictureVO> pagePictures(PictureQueryRequest request, Long currentUserId) {
        return page(request, null, currentUserId);
    }

    @Override
    public IPage<PictureVO> pageMyPictures(PictureQueryRequest request, Long userId) {
        return page(request, userId, userId);
    }

    private IPage<PictureVO> page(PictureQueryRequest request, Long ownerUserId, Long currentUserId) {
        Page<Picture> page = new Page<>(request.getCurrent(), request.getPageSize());

        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Picture::getIsDelete, 0);
        if (StringUtils.isNotBlank(request.getName())) {
            wrapper.like(Picture::getName, request.getName());
        }
        if (StringUtils.isNotBlank(request.getDescription())) {
            wrapper.like(Picture::getDescription, request.getDescription());
        }
        if (request.getMinSize() != null) {
            wrapper.ge(Picture::getSize, request.getMinSize());
        }
        if (request.getMaxSize() != null) {
            wrapper.le(Picture::getSize, request.getMaxSize());
        }
        if (ownerUserId != null) {
            wrapper.eq(Picture::getUserId, ownerUserId);
        }

        if (StringUtils.isNotBlank(request.getSortField())) {
            SFunction<Picture, ?> column = SORT_FIELD_MAP.get(request.getSortField());
            if (column != null) {
                boolean asc = "asc".equalsIgnoreCase(request.getSortOrder());
                wrapper.orderBy(true, asc, column);
            }
        } else {
            wrapper.orderByDesc(Picture::getCreateTime);
        }

        Page<Picture> result = pictureMapper.selectPage(page, wrapper);
        IPage<PictureVO> voPage = result.convert(PictureConverter::toVO);

        List<PictureVO> records = voPage.getRecords();
        if (org.springframework.util.CollectionUtils.isEmpty(records)) {
            return voPage;
        }

        Set<Long> userIds = records.stream()
                .map(PictureVO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            Map<Long, UserVO> userVOMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, UserConverter::toVO));
            records.forEach(vo -> vo.setUser(userVOMap.get(vo.getUserId())));
        }

        enrichLikeFields(records, currentUserId);
        return voPage;
    }

    @Override
    public PictureVO updatePicture(PictureUpdateRequest request, Long userId) {
        Long id = request.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 ID 不能为空");
        }
        if (StringUtils.isBlank(request.getName()) && StringUtils.isBlank(request.getDescription())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片名称和简介不能同时为空");
        }

        Picture picture = pictureMapper.selectById(id);
        if (picture == null || picture.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        if (!picture.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能编辑自己的图片");
        }

        if (StringUtils.isNotBlank(request.getName())) {
            picture.setName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getDescription())) {
            picture.setDescription(request.getDescription());
        }
        pictureMapper.updateById(picture);
        PictureVO vo = PictureConverter.toVO(picture);
        User user = userMapper.selectById(userId);
        vo.setUser(UserConverter.toVO(user));
        enrichLikeFields(List.of(vo), userId);
        return vo;
    }

    @Override
    public void deletePicture(Long id, Long userId) {
        Picture picture = pictureMapper.selectById(id);
        if (picture == null || picture.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        if (!picture.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能删除自己的图片");
        }

        String bucket = cosProperties.getBucket();
        String key = extractKey(picture.getUrl());
        try {
            cosClient.deleteObject(bucket, key);
        } catch (Exception e) {
            log.error("删除 COS 文件失败, bucket={}, key={}", bucket, key, e);
        }

        pictureMapper.deleteById(id);
        log.info("图片删除成功 id={}", id);
    }

    @Override
    public PictureVO getPictureById(Long id, Long currentUserId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 ID 不能为空");
        }
        Picture picture = pictureMapper.selectById(id);
        if (picture == null || picture.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        PictureVO vo = PictureConverter.toVO(picture);
        if (picture.getUserId() != null) {
            User user = userMapper.selectById(picture.getUserId());
            vo.setUser(UserConverter.toVO(user));
        }
        enrichLikeFields(List.of(vo), currentUserId);
        return vo;
    }

    private void enrichLikeFields(List<PictureVO> records, Long currentUserId) {
        if (org.springframework.util.CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> pictureIds = records.stream()
                .map(PictureVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Long> countMap = pictureLikeService.countByPictureIds(pictureIds);
        Set<Long> likedIds = pictureLikeService.findLikedPictureIds(currentUserId, pictureIds);
        for (PictureVO vo : records) {
            vo.setLikeCount(countMap.getOrDefault(vo.getId(), 0L));
            vo.setLiked(likedIds.contains(vo.getId()));
        }
    }

    private String extractKey(String url) {
        String baseUrl = cosProperties.getBaseUrl();
        if (url != null && url.startsWith(baseUrl)) {
            return url.substring(baseUrl.length() + 1);
        }
        return url;
    }

    private String generateKey(String ext) {
        LocalDate today = LocalDate.now();
        return String.format("%d/%02d/%02d/%s%s",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                ext);
    }
}
