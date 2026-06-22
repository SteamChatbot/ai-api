package com.sanavi.aiapi.analysis.mapper;

import com.sanavi.aiapi.analysis.dto.AnalysisHistoryItemDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResultDto;
import com.sanavi.aiapi.analysis.dto.ChecklistItemDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnalysisResultMapper {
    void insertAnalysisResult(AnalysisResultDto dto);
    void updateBaseScore(@Param("id") String id, @Param("baseScore") float baseScore);
    boolean existsChatByResultId(@Param("resultId") String resultId);
    void insertChat(@Param("resultId") String resultId, @Param("chat_content") String chat_content);
    void insertChecklist(@Param("resultId") String resultId, @Param("items") List<ChecklistItemDto> items);
    void insertWarnings(@Param("resultId") String resultId, @Param("warnings") List<String> warnings);
    void insertMetaContents(@Param("resultId") String resultId, @Param("metaContents") List<String> metaContents);
    List<AnalysisHistoryItemDto> findHistoryByUserId(@Param("userId") String userId);
    int softDeleteById(@Param("id") String id, @Param("userId") String userId);
}
