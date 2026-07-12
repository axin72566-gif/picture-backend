package com.example.picturebackend.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.space.entity.SpaceMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface SpaceMessageMapper extends BaseMapper<SpaceMessage> {

    /**
     * 批量查询消息（含已软删），用于组装 replyTo 摘要。
     */
    @Select({
            "<script>",
            "SELECT * FROM space_message",
            "WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<SpaceMessage> selectBatchIdsIncludeDeleted(@Param("ids") Collection<Long> ids);
}
