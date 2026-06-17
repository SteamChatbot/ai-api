package com.sanavi.aiapi.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;



@EqualsAndHashCode
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisRequestDto {
    @NotBlank private String name; //유저명
    @NotNull  private Integer age; //유저나이
    @NotBlank private String job; //유저 직업(직종)
    @NotBlank private String disease; //유저 질병명
    @NotBlank private String inspector; //유저 사고경위
}
