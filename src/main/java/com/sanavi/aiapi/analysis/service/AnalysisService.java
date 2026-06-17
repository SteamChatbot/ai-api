package com.sanavi.aiapi.analysis.service;

import com.sanavi.aiapi.analysis.dto.AnalysisAcceptedDto;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;

public interface AnalysisService {
    AnalysisAcceptedDto requestAnalysis(AnalysisRequestDto request);
    AnalysisResponseDto getAnalysisResult(String taskId);
}
