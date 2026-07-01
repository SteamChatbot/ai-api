package com.sanavi.aiapi.admin.mapper;

import com.sanavi.aiapi.admin.dto.AdminAnalysisItemDto;
import com.sanavi.aiapi.admin.dto.TrendPointDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminAnalysisMapper {
    // 관리자 전용 — 전체 회원 분석이력 (소유자 무관)
    List<AdminAnalysisItemDto> selectAdminAnalysisList(
            @Param("keyword") String keyword,
            @Param("scoreFilter") String scoreFilter,
            @Param("offset") int offset,
            @Param("size") int size);

    int selectAdminAnalysisCount(
            @Param("keyword") String keyword,
            @Param("scoreFilter") String scoreFilter);

    Double selectAdminAnalysisAverage(
            @Param("keyword") String keyword,
            @Param("scoreFilter") String scoreFilter);

    int forceSoftDeleteById(@Param("id") String id);

    // 관리자 통계 — 일별(최근 7일)/월별(최근 6개월) 분석횟수 추이. startDate는 서비스 레이어에서 계산
    List<TrendPointDto> selectAnalysisTrend(@Param("range") String range, @Param("startDate") LocalDateTime startDate);

    // 관리자 통계 — 질병/직업별 TOP 랭킹. by는 서비스 레이어에서 "disease"/"job"만 허용하도록 검증된 값만 전달됨
    List<TrendPointDto> selectAnalysisRanking(@Param("by") String by, @Param("limit") int limit);

    // 관리자 다중필터 조합 검색 — backend-springboot가 main_db(구독여부·유저타입·직업)로 먼저 추려낸
    // userIds에 대해, 그 유저들의 기간 내 분석 요청 총건수만 집계해서 돌려줌 (ai_db는 여기까지만 담당)
    int selectAnalysisCountForUsers(@Param("startDate") LocalDateTime startDate, @Param("userIds") List<String> userIds);
}
