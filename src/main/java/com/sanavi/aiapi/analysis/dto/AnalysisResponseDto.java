package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// FastAPI → Spring → React: 폴링 응답 — status(PROCESSING/COMPLETED/ERROR)와 분석 data를 감싸는 외부 DTO
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResponseDto {
    private boolean success; //성공여부
    private String message; //에러메세지
    private String status; //요청상태
    private AnalysisDataDto data; //요청데이타
}
