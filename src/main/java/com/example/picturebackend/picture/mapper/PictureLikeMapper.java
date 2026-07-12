package com.example.picturebackend.picture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.picture.entity.PictureLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PictureLikeMapper extends BaseMapper<PictureLike> {

    /**
     * 物理删除点赞关系（绕过 @TableLogic 软删除）。
     * 点赞关系有唯一索引 uk(userId, pictureId)，软删除会导致无法再次点赞。
     */
    @Delete("DELETE FROM picture_like WHERE userId = #{userId} AND pictureId = #{pictureId}")
    int deletePhysically(@Param("userId") Long userId, @Param("pictureId") Long pictureId);

    /**
     * 恢复历史上软删除的点赞记录（兼容旧数据）。
     */
    @Update("""
            UPDATE picture_like
            SET isDelete = 0, createTime = NOW()
            WHERE userId = #{userId}
              AND pictureId = #{pictureId}
              AND isDelete = 1
            """)
    int restoreSoftDeleted(@Param("userId") Long userId, @Param("pictureId") Long pictureId);
}
