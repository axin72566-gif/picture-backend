package com.example.picturebackend.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.space.entity.SpaceMember;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SpaceMemberMapper extends BaseMapper<SpaceMember> {

    /**
     * 物理删除成员关系（绕过 @TableLogic），避免唯一索引 uk_space_user 被软删占用。
     */
    @Delete("DELETE FROM space_member WHERE spaceId = #{spaceId} AND userId = #{userId}")
    int deletePhysically(@Param("spaceId") Long spaceId, @Param("userId") Long userId);

    /**
     * 物理删除某空间全部成员。
     */
    @Delete("DELETE FROM space_member WHERE spaceId = #{spaceId}")
    int deleteAllBySpaceIdPhysically(@Param("spaceId") Long spaceId);
}
