package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
