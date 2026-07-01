package com.sanavi.aiapi.admin.service;

import com.sanavi.aiapi.admin.dto.AdminAnalysisItemDto;
import com.sanavi.aiapi.admin.dto.AdminAnalysisStatsDto;
import com.sanavi.aiapi.admin.dto.AnalysisCountResultDto;
import com.sanavi.aiapi.admin.dto.CountForUsersRequestDto;
import com.sanavi.aiapi.admin.dto.TrendPointDto;
import com.sanavi.aiapi.admin.mapper.AdminAnalysisMapper;
import com.sanavi.aiapi.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

// 관리자 전용 — 전체 회원 분석이력 조회/통계/추이/랭킹/강제삭제 (소유자 검증 없음)
// backend-springboot의 관리자 프록시 컨트롤러만 호출하도록 의도됨 — role_admin 권한 검사는
// backend-springboot SecurityConfig에서 처리 (현재 SecurityConfig가 permitAll 상태라 활성화 전까지는
// 이 서비스가 사실상 보호되지 않으니, 권한 규칙부터 켤 것)
@Service
@RequiredArgsConstructor
public class AdminAnalysisServiceImpl implements AdminAnalysisService {

    private final AdminAnalysisMapper adminAnalysisMapper;

    private static final Set<String> ALLOWED_RANKING_COLUMNS = Set.of("disease", "job");

    // Input:  keyword(회원ID/질병명/직업 검색), scoreFilter(high/mid/low), page, size
    // Output: PageResponse<AdminAnalysisItemDto> — 소유자 무관 전체 분석이력
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminAnalysisItemDto> getAdminAnalysisList(String keyword, String scoreFilter, int page, int size) {
        int offset = (page - 1) * size;
        List<AdminAnalysisItemDto> contents = adminAnalysisMapper.selectAdminAnalysisList(keyword, scoreFilter, offset, size);
        int totalCount = adminAnalysisMapper.selectAdminAnalysisCount(keyword, scoreFilter);
        return new PageResponse<>(contents, page, size, totalCount);
    }

    // Input:  keyword, scoreFilter (검색결과 평균 계산용 — 둘 다 비어있으면 filteredCount=0, filteredAverage=null)
    // Output: AdminAnalysisStatsDto — 전체 평균/건수 + 검색결과 평균/건수
    @Override
    @Transactional(readOnly = true)
    public AdminAnalysisStatsDto getAdminAnalysisStats(String keyword, String scoreFilter) {
        int overallCount = adminAnalysisMapper.selectAdminAnalysisCount(null, null);
        Double overallAverage = overallCount > 0 ? adminAnalysisMapper.selectAdminAnalysisAverage(null, null) : null;

        boolean isFiltered = (keyword != null && !keyword.isBlank()) || (scoreFilter != null && !scoreFilter.isBlank());
        int filteredCount = isFiltered ? adminAnalysisMapper.selectAdminAnalysisCount(keyword, scoreFilter) : 0;
        Double filteredAverage = (isFiltered && filteredCount > 0)
                ? adminAnalysisMapper.selectAdminAnalysisAverage(keyword, scoreFilter)
                : null;

        return AdminAnalysisStatsDto.builder()
                .overallAverage(overallAverage)
                .overallCount(overallCount)
                .filteredAverage(filteredAverage)
                .filteredCount(filteredCount)
                .build();
    }

    // Input:  taskId — 소유자 무관 강제삭제 (관리자 전용, userId 검증 없음)
    // Output: void — 대상 없으면 404
    @Override
    @Transactional
    public void forceDeleteAnalysis(String taskId) {
        int affected = adminAnalysisMapper.forceSoftDeleteById(taskId);
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "분석 결과를 찾을 수 없습니다.");
        }
    }

    // Input:  range ("daily" 최근 7일 / "monthly" 최근 6개월, 그 외 값은 daily로 처리)
    // Output: List<TrendPointDto> — 통계 페이지 분석횟수 추이 그래프용
    // 책임:   날짜 경계는 앱 서버 타임존 기준으로 여기서 계산해 SQL에는 값만 바인딩 (DB 세션 타임존에 의존하지 않도록)
    @Override
    @Transactional(readOnly = true)
    public List<TrendPointDto> getAnalysisTrend(String range) {
        LocalDateTime startDate = "monthly".equals(range)
                ? LocalDate.now().minusMonths(5).atStartOfDay()
                : LocalDate.now().minusDays(6).atStartOfDay();
        return adminAnalysisMapper.selectAnalysisTrend(range, startDate);
    }

    // Input:  by ("disease" | "job" — 그 외 값은 거부), limit (TOP N)
    // Output: List<TrendPointDto> — 질병/직업별 분석 요청 건수 TOP 랭킹
    // 책임:   by는 SQL 컬럼명으로 그대로 삽입되므로(${by}) 화이트리스트 검증 필수
    @Override
    @Transactional(readOnly = true)
    public List<TrendPointDto> getAnalysisRanking(String by, int limit) {
        if (!ALLOWED_RANKING_COLUMNS.contains(by)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 랭킹 기준입니다.");
        }
        return adminAnalysisMapper.selectAnalysisRanking(by, limit);
    }

    // Input:  CountForUsersRequestDto — range("week"/"month"/"halfyear") + backend-springboot가
    //         main_db(구독여부·유저타입·직업)로 이미 추려낸 userIds
    // Output: AnalysisCountResultDto — 그 유저들의 기간 내 분석 요청 총건수
    // 책임:   ai-api는 필터링을 하지 않고 이미 정해진 userIds에 대한 집계만 담당 (필터 조건은 main_db 소관)
    @Override
    @Transactional(readOnly = true)
    public AnalysisCountResultDto getAnalysisCountForUsers(CountForUsersRequestDto request) {
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = switch (request.getRange()) {
            case "month" -> today.minusMonths(1).atStartOfDay();
            case "halfyear" -> today.minusMonths(6).atStartOfDay();
            default -> today.minusDays(6).atStartOfDay();
        };
        List<String> userIds = request.getUserIds();
        int totalCount = (userIds == null || userIds.isEmpty())
                ? 0
                : adminAnalysisMapper.selectAnalysisCountForUsers(startDate, userIds);
        return new AnalysisCountResultDto(totalCount);
    }
}
