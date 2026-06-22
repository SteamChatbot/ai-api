package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// FastAPI → Spring → ai_db: 증거 체크리스트 단일 항목 — AnalysisDataDto.checklist 요소 및 DB 삽입에 공용
@EqualsAndHashCode
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChecklistItemDto {
    private int id; // 증거목록 id
    private String label; //증거목록 제목
    private String purpose; //목적
    private String method; //방법
    private String reason; //이유
}
