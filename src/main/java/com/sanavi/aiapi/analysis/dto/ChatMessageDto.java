package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// React → Spring: 대화 히스토리 단일 메시지 — role("user"|"ai") + content
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private String role;    // "user" | "ai"
    private String content;
}
