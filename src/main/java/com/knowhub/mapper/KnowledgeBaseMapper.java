package com.knowhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowhub.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    @Update("UPDATE knowledge_base SET doc_count = doc_count + 1 WHERE id = #{id}")
    void incrementDocCount(@Param("id") Long id);
}
