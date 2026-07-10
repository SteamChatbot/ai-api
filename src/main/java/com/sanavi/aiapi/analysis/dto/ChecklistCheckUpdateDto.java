package com.sanavi.aiapi.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChecklistCheckUpdateDto {

    private int id;
    private boolean checked;
}