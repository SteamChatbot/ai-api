package com.sanavi.aiapi.analysis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// React → Spring → FastAPI: 추가 질의 요청 — context(캐시) + history(이전 Q&A) + question(새 질문)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDto {

    @NotNull @Valid
    private ChatContextDto context;

    private List<ChatMessageDto> history;

    @NotBlank
    private String question;
}
