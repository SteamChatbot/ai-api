package com.sanavi.aiapi.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// FastAPI → Spring: 분석 완료 시 실제 결과 데이터 — AnalysisResponseDto.data 필드로 중첩됨
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

    // 2026-07-09 추가 — DB(loadFromDb) 경로에서 조회 시 비어있어서 프론트 상세페이지/PDF에 직업·질병명이
    // 안 나오던 문제 수정. FastAPI 즉시응답(pollFastApi) 경로는 원래 값이 채워져 있을 수 있어 필드만 추가.
    private String job;
    private String disease;
    private String inspector;
}
