package com.sanavi.aiapi.analysis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChecklistSaveRequestDto {

    @Valid
    @NotEmpty
    private List<ChecklistCheckUpdateDto> items;
}