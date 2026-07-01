package com.sanavi.aiapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 관리자 전용 — 전체 평균 승인율 / 검색·필터 결과 평균 승인율
// filteredAverage·filteredCount는 keyword/scoreFilter 둘 다 비어있으면 0건으로 응답 (프론트가 "—" 표시)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminAnalysisStatsDto {
    private Double overallAverage;
    private int overallCount;
    private Double filteredAverage;
    private int filteredCount;
}
