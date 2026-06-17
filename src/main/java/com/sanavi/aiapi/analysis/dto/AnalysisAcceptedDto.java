package com.sanavi.aiapi.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisAcceptedDto {

    @JsonProperty("task_id")
    private String taskId; //analysis_result id pk값

    private String status; //상태
}
