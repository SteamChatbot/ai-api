package com.sanavi.aiapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 관리자 전용 — 전체 회원 분석이력 목록 한 행 (AnalysisHistoryItemDto와 달리 userId 포함)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminAnalysisItemDto {
    private String id;
    private String userId;
    private String disease;
    private String job;
    private float baseScore;
    private LocalDateTime createdAt;
}
