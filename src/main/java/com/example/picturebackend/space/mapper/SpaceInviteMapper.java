package com.example.picturebackend.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.space.entity.SpaceInvite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SpaceInviteMapper extends BaseMapper<SpaceInvite> {

    /**
     * 物理删除某空间全部待处理邀请（解散空间时）。
     */
    @Delete("DELETE FROM space_invite WHERE spaceId = #{spaceId} AND status = 'PENDING'")
    int deletePendingBySpaceIdPhysically(@Param("spaceId") Long spaceId);
}
