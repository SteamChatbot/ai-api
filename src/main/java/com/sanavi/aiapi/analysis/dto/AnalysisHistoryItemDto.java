package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ai_db → Spring → React: 마이페이지 분석 이력 목록 한 행
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisHistoryItemDto {
    private String id; //분
    private String disease;
    private String job;
    private float baseScore;
    private LocalDateTime createdAt;
}
