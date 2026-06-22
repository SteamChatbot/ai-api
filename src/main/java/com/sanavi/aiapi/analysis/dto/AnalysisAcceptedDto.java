package com.sanavi.aiapi.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// FastAPI → Spring: 분석 요청 접수 응답 — task_id와 PROCESSING 상태를 React에 전달
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisAcceptedDto {

    @JsonProperty("task_id")
    private String taskId; //analysis_result id pk값

    private String status; //상태
}
