package com.sanavi.aiapi.admin.service;

import com.sanavi.aiapi.admin.dto.AdminAnalysisItemDto;
import com.sanavi.aiapi.admin.dto.AdminAnalysisStatsDto;
import com.sanavi.aiapi.admin.dto.AnalysisCountResultDto;
import com.sanavi.aiapi.admin.dto.CountForUsersRequestDto;
import com.sanavi.aiapi.admin.dto.TrendPointDto;
import com.sanavi.aiapi.common.dto.PageResponse;

import java.util.List;

public interface AdminAnalysisService {
    PageResponse<AdminAnalysisItemDto> getAdminAnalysisList(String keyword, String scoreFilter, int page, int size);
    AdminAnalysisStatsDto getAdminAnalysisStats(String keyword, String scoreFilter);
    void forceDeleteAnalysis(String taskId);
    List<TrendPointDto> getAnalysisTrend(String range);
    List<TrendPointDto> getAnalysisRanking(String by, int limit);
    AnalysisCountResultDto getAnalysisCountForUsers(CountForUsersRequestDto request);
}
