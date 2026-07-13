package com.sanavi.aiapi.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;



// React → Spring: 분석 요청 시 유저가 입력한 정보를 담는 DTO
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
    @NotBlank @Size(max = 1000, message = "사고경위는 1000자 이내로 입력해주세요.") private String inspector; //유저 사고경위
}
