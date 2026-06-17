package com.sanavi.aiapi.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisDataDto {
    private String analysisId;

    @JsonProperty("base_score")
    private float baseScore; //산출율 float %로표현 추후

    @JsonProperty("chat_content")
    private String chatContent; //ai답변

    private List<ChecklistItemDto> checklist; //증거목록 id별로 3~5가지제공 list

    private List<String> warning; //주의사항 3가지제공 list

    @JsonProperty("meta_content")
    private List<String> metaContent; //참고판례데이터 list 추후 추가된다면 Dto별도설정필요

    private LocalDateTime createdAt; //생성시간
}
