package com.sanavi.aiapi.analysis.service;

import com.sanavi.aiapi.analysis.dto.AnalysisAcceptedDto;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;
import com.sanavi.aiapi.analysis.dto.ChatRequestDto;
import com.sanavi.aiapi.analysis.dto.ChatResponseDto;

public interface AnalysisService {
    AnalysisAcceptedDto requestAnalysis(AnalysisRequestDto request);
    AnalysisResponseDto getAnalysisResult(String taskId);
    ChatResponseDto chatWithAdvisor(ChatRequestDto request);
}
