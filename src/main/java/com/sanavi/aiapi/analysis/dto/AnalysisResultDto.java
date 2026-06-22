package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Spring → ai_db: 분석 요청 접수 시 analysis_result 테이블에 초기 레코드 삽입용 DTO
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResultDto {
    private String id;
    private String userId;
    private String disease;
    private String inspector;
    private String job;
    private float baseScore;
    private LocalDateTime createdAt;
    private int deleted;
}
