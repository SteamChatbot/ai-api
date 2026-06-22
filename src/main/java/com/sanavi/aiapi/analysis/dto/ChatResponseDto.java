package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// FastAPI → Spring → React: 추가 질의 응답 — LLM이 생성한 answer 문자열
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponseDto {
    private String answer;
}
