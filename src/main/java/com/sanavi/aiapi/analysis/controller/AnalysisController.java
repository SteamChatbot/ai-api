package com.sanavi.aiapi.analysis.controller;

import com.sanavi.aiapi.analysis.dto.AnalysisAcceptedDto;
import com.sanavi.aiapi.analysis.dto.AnalysisHistoryItemDto;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;
import com.sanavi.aiapi.analysis.dto.ChatRequestDto;
import com.sanavi.aiapi.analysis.dto.ChatResponseDto;
import com.sanavi.aiapi.analysis.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // Input:  AnalysisRequestDto (이름, 나이, 직업, 질병, 사고경위) + X-User-Id 헤더 (backend-springboot가 주입)
    // Output: AnalysisAcceptedDto (task_id, status="PROCESSING")
    // 책임:   FastAPI에 분석 요청 중계 + ai_db에 userId 포함 초기 레코드 삽입
    @PostMapping
    public ResponseEntity<AnalysisAcceptedDto> requestAnalysis(
            @RequestBody @Valid AnalysisRequestDto request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(analysisService.requestAnalysis(request, userId));
    }

    // Input:  taskId (UUID — analysis_result PK)
    // Output: AnalysisResponseDto (status, data)
    // 책임:   FastAPI Redis에서 분석 결과 조회 + COMPLETED 시 ai_db에 결과 저장 (중복 방지)
    @GetMapping("/{taskId}")
    public ResponseEntity<AnalysisResponseDto> getAnalysisResult(@PathVariable("taskId") String taskId) {
        return ResponseEntity.ok(analysisService.getAnalysisResult(taskId));
    }

    // Input:  ChatRequestDto (context, history, question)
    // Output: ChatResponseDto (answer)
    // 책임:   브라우저 캐시 컨텍스트 + 대화 히스토리를 FastAPI에 중계해 AI 추가 질의 응답
    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chat(@RequestBody @Valid ChatRequestDto request) {
        return ResponseEntity.ok(analysisService.chatWithAdvisor(request));
    }

    // Input:  userId (로그인 유저 ID — PathVariable)
    // Output: List<AnalysisHistoryItemDto> (id, disease, job, baseScore, createdAt)
    // 책임:   ai_db에서 해당 유저의 분석 이력 최신순 조회
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<AnalysisHistoryItemDto>> getHistory(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(analysisService.getMyHistory(userId));
    }

    // Input:  taskId (analysis_result PK), userId (소유권 검증용 쿼리파라미터)
    // Output: 204 No Content
    // 책임:   논리 삭제 (deleted=0) — userId 불일치 시 404
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteAnalysis(
            @PathVariable("taskId") String taskId,
            @RequestParam("userId") String userId) {
        analysisService.softDeleteAnalysis(taskId, userId);
        return ResponseEntity.noContent().build();
    }
}
