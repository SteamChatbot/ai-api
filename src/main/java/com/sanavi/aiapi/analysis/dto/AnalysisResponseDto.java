package com.sanavi.aiapi.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AnalysisResponseDto {

    @JsonProperty("base_score")
    private float baseScore;

    @JsonProperty("chat_content")
    private String chatContent;

    private List<ChecklistItemDto> checklist;

    private List<String> warning;

    @JsonProperty("meta_content")
    private List<String> metaContent;
}
