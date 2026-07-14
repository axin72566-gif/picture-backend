package com.example.picturebackend.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.chat.entity.SensitiveWord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    /** 物理删除，避免软删占用 uk_word 导致无法再次添加同一词 */
    @Delete("DELETE FROM sensitive_word WHERE id = #{id}")
    int deletePhysically(@Param("id") Long id);
}
