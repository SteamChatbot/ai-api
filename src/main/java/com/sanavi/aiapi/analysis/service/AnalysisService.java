package com.sanavi.aiapi.analysis.service;

import java.util.List;

import com.sanavi.aiapi.analysis.dto.AnalysisAcceptedDto;
import com.sanavi.aiapi.analysis.dto.AnalysisHistoryItemDto;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;
import com.sanavi.aiapi.analysis.dto.ChatRequestDto;
import com.sanavi.aiapi.analysis.dto.ChatResponseDto;
import com.sanavi.aiapi.analysis.dto.ChecklistSaveRequestDto;

public interface AnalysisService {
    AnalysisAcceptedDto requestAnalysis(AnalysisRequestDto request, String userId);

    AnalysisResponseDto getAnalysisResult(String taskId);

    ChatResponseDto chatWithAdvisor(ChatRequestDto request);

    List<AnalysisHistoryItemDto> getMyHistory(String userId);

    void softDeleteAnalysis(String taskId, String userId);

    void saveChecklistChecks(
            String taskId,
            String userId,
            ChecklistSaveRequestDto request);
}
