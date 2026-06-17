package com.sanavi.aiapi.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnalysisRequestDto {
    @NotBlank private String name;
    @NotNull  private Integer age;
    @NotBlank private String job;
    @NotBlank private String disease;
    @NotBlank private String inspector;
}
