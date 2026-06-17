package com.sanavi.aiapi.analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChecklistItemDto {
    private int id;
    private String label;
    private String purpose;
    private String method;
    private String reason;
}
