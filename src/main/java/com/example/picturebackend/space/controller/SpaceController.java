package com.example.picturebackend.space.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.common.PageRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.picture.model.dto.PictureQueryRequest;
import com.example.picturebackend.picture.model.vo.PictureVO;
import com.example.picturebackend.picture.service.PictureService;
import com.example.picturebackend.space.model.dto.SpaceCreateRequest;
import com.example.picturebackend.space.model.dto.SpaceInviteRequest;
import com.example.picturebackend.space.model.dto.SpaceMemberRoleUpdateRequest;
import com.example.picturebackend.space.model.dto.SpaceUpdateRequest;
import com.example.picturebackend.space.model.vo.SpaceInviteVO;
import com.example.picturebackend.space.model.vo.SpaceMemberVO;
import com.example.picturebackend.space.model.vo.SpaceVO;
import com.example.picturebackend.space.service.SpaceInviteService;
import com.example.picturebackend.space.service.SpaceMemberService;
import com.example.picturebackend.space.service.SpaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/space")
public class SpaceController {

    private final SpaceService spaceService;

    private final SpaceMemberService spaceMemberService;

    private final SpaceInviteService spaceInviteService;

    private final PictureService pictureService;

    public SpaceController(SpaceService spaceService,
                           SpaceMemberService spaceMemberService,
                           SpaceInviteService spaceInviteService,
                           PictureService pictureService) {
        this.spaceService = spaceService;
        this.spaceMemberService = spaceMemberService;
        this.spaceInviteService = spaceInviteService;
        this.pictureService = pictureService;
    }

    @PostMapping
    public BaseResponse<SpaceVO> create(@RequestBody SpaceCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceService.createSpace(request, userId));
    }

    @GetMapping("/my")
    public BaseResponse<IPage<SpaceVO>> pageMySpaces(PageRequest pageRequest, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceService.pageMySpaces(userId, pageRequest));
    }

    @GetMapping("/invites/pending")
    public BaseResponse<IPage<SpaceInviteVO>> pageMyPendingInvites(PageRequest pageRequest,
                                                                   HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceInviteService.pageMyPendingInvites(userId, pageRequest));
    }

    @PostMapping("/invites/{inviteId:\\d+}/accept")
    public BaseResponse<Void> acceptInvite(@PathVariable Long inviteId, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        spaceInviteService.accept(inviteId, userId);
        return ResultUtils.success(null);
    }

    @PostMapping("/invites/{inviteId:\\d+}/reject")
    public BaseResponse<Void> rejectInvite(@PathVariable Long inviteId, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        spaceInviteService.reject(inviteId, userId);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/invites/{inviteId:\\d+}")
    public BaseResponse<Void> cancelInvite(@PathVariable Long inviteId, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        spaceInviteService.cancel(inviteId, userId);
        return ResultUtils.success(null);
    }

    @GetMapping("/{id:\\d+}")
    public BaseResponse<SpaceVO> getSpace(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceService.getSpace(id, userId));
    }

    @PutMapping("/{id:\\d+}")
    public BaseResponse<SpaceVO> updateSpace(@PathVariable Long id,
                                             @RequestBody SpaceUpdateRequest request,
                                             HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceService.updateSpace(id, request, userId));
    }

    @DeleteMapping("/{id:\\d+}")
    public BaseResponse<Void> dissolveSpace(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        spaceService.dissolveSpace(id, userId);
        return ResultUtils.success(null);
    }

    @GetMapping("/{id:\\d+}/pictures")
    public BaseResponse<IPage<PictureVO>> pageSpacePictures(@PathVariable Long id,
                                                            PictureQueryRequest request,
                                                            HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(pictureService.pageSpacePictures(id, request, userId));
    }

    @GetMapping("/{id:\\d+}/members")
    public BaseResponse<IPage<SpaceMemberVO>> pageMembers(@PathVariable Long id,
                                                          PageRequest pageRequest,
                                                          HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceMemberService.pageMembers(id, userId, pageRequest));
    }

    @PutMapping("/{id:\\d+}/members/{userId:\\d+}/role")
    public BaseResponse<Void> updateMemberRole(@PathVariable Long id,
                                               @PathVariable Long userId,
                                               @RequestBody SpaceMemberRoleUpdateRequest request,
                                               HttpServletRequest httpRequest) {
        Long operatorId = requireLoginUserId(httpRequest);
        String role = request != null ? request.getRole() : null;
        spaceMemberService.updateRole(id, userId, role, operatorId);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id:\\d+}/members/me")
    public BaseResponse<Void> leaveSpace(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        spaceMemberService.leave(id, userId);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id:\\d+}/members/{userId:\\d+}")
    public BaseResponse<Void> removeMember(@PathVariable Long id,
                                           @PathVariable Long userId,
                                           HttpServletRequest httpRequest) {
        Long operatorId = requireLoginUserId(httpRequest);
        spaceMemberService.removeMember(id, userId, operatorId);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id:\\d+}/invites")
    public BaseResponse<SpaceInviteVO> invite(@PathVariable Long id,
                                              @RequestBody SpaceInviteRequest request,
                                              HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceInviteService.invite(id, request, userId));
    }

    @GetMapping("/{id:\\d+}/invites")
    public BaseResponse<IPage<SpaceInviteVO>> pageSpaceInvites(@PathVariable Long id,
                                                               PageRequest pageRequest,
                                                               HttpServletRequest httpRequest) {
        Long userId = requireLoginUserId(httpRequest);
        return ResultUtils.success(spaceInviteService.pageSpacePendingInvites(id, userId, pageRequest));
    }

    private Long requireLoginUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return userId;
    }
}
