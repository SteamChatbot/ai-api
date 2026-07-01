package com.sanavi.aiapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 관리자 통계 — 일별/월별 집계 한 포인트 (분석횟수 추이, 랭킹 공용)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrendPointDto {
    private String label;
    private int count;
}
