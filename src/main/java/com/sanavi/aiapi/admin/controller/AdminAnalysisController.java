package com.sanavi.aiapi.admin.controller;

import java.util.List;

import com.sanavi.aiapi.admin.dto.AdminAnalysisItemDto;
import com.sanavi.aiapi.admin.dto.AdminAnalysisStatsDto;
import com.sanavi.aiapi.admin.dto.AnalysisCountResultDto;
import com.sanavi.aiapi.admin.dto.CountForUsersRequestDto;
import com.sanavi.aiapi.admin.dto.TrendPointDto;
import com.sanavi.aiapi.admin.service.AdminAnalysisService;
import com.sanavi.aiapi.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 관리자 전용 — 전체 회원 분석이력 조회/통계/강제삭제 (소유자 검증 없음)
// backend-springboot의 관리자 프록시 컨트롤러만 호출하도록 의도됨 — role_admin 권한 검사는
// backend-springboot SecurityConfig에서 처리 (현재 SecurityConfig가 permitAll 상태라 활성화 전까지는
// 이 엔드포인트가 사실상 보호되지 않으니, 권한 규칙부터 켤 것)
@RestController
@RequestMapping("/api/admin/analysis")
@RequiredArgsConstructor
public class AdminAnalysisController {

    private final AdminAnalysisService adminAnalysisService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminAnalysisItemDto>> getAdminAnalysisList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "scoreFilter", required = false) String scoreFilter) {
        return ResponseEntity.ok(adminAnalysisService.getAdminAnalysisList(keyword, scoreFilter, page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminAnalysisStatsDto> getAdminAnalysisStats(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "scoreFilter", required = false) String scoreFilter) {
        return ResponseEntity.ok(adminAnalysisService.getAdminAnalysisStats(keyword, scoreFilter));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> forceDeleteAnalysis(@PathVariable("taskId") String taskId) {
        adminAnalysisService.forceDeleteAnalysis(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trend")
    public ResponseEntity<List<TrendPointDto>> getAnalysisTrend(
            @RequestParam(name = "range", defaultValue = "daily") String range) {
        return ResponseEntity.ok(adminAnalysisService.getAnalysisTrend(range));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<TrendPointDto>> getAnalysisRanking(
            @RequestParam(name = "by", defaultValue = "disease") String by,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminAnalysisService.getAnalysisRanking(by, limit));
    }

    // 관리자 다중필터 조합 검색용 — backend-springboot가 main_db(구독여부·유저타입·직업)로 이미
    // 추려낸 userIds를 넘기면, 그 유저들의 기간 내 분석 요청 총건수만 집계해서 돌려줌
    @PostMapping("/count-for-users")
    public ResponseEntity<AnalysisCountResultDto> getAnalysisCountForUsers(
            @RequestBody CountForUsersRequestDto request) {
        return ResponseEntity.ok(adminAnalysisService.getAnalysisCountForUsers(request));
    }
}
