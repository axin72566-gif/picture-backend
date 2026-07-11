package com.example.picturebackend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.user.entity.UserFollow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * 物理删除关注关系（绕过 @TableLogic 软删除）。
     * 关注关系有唯一索引 uk(followerId, followedId)，软删除会导致无法再次关注。
     */
    @Delete("DELETE FROM user_follow WHERE followerId = #{followerId} AND followedId = #{followedId}")
    int deletePhysically(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

    /**
     * 恢复历史上软删除的关注记录（兼容旧数据）。
     */
    @Update("""
            UPDATE user_follow
            SET isDelete = 0, createTime = NOW()
            WHERE followerId = #{followerId}
              AND followedId = #{followedId}
              AND isDelete = 1
            """)
    int restoreSoftDeleted(@Param("followerId") Long followerId, @Param("followedId") Long followedId);
}
