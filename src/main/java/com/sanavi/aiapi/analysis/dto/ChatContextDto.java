package com.sanavi.aiapi.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// React → Spring: 브라우저에 캐싱된 분석 컨텍스트 — 추가 질의 시 매 요청마다 전송
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatContextDto {

    @JsonProperty("chat_content")
    private String chatContent;

    private List<ChecklistItemDto> checklist;

    private List<String> warning;
}
