package com.sanavi.aiapi.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanavi.aiapi.analysis.dto.AnalysisAcceptedDto;
import com.sanavi.aiapi.analysis.dto.AnalysisDataDto;
import com.sanavi.aiapi.analysis.dto.AnalysisHistoryItemDto;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResultDto;
import com.sanavi.aiapi.analysis.dto.ChatRequestDto;
import com.sanavi.aiapi.analysis.dto.ChatResponseDto;
import com.sanavi.aiapi.analysis.mapper.AnalysisResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final RestClient fastApiClient;
    private final ObjectMapper objectMapper;
    private final AnalysisResultMapper analysisResultMapper;

    // Input:  AnalysisRequestDto (유저 입력값) + userId (X-User-Id 헤더, 비로그인 시 null)
    // Output: AnalysisAcceptedDto (task_id, status="PROCESSING")
    // 책임:   FastAPI에 분석 요청 중계 → task_id 수신 → ai_db analysis_result 초기 레코드 삽입
    @Override
    @Transactional
    public AnalysisAcceptedDto requestAnalysis(AnalysisRequestDto request, String userId) {
        try {
            AnalysisAcceptedDto accepted = fastApiClient.post()
                .uri("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AnalysisAcceptedDto.class);

            String resolvedUserId = (userId != null && !userId.isBlank()) ? userId : null;

            analysisResultMapper.insertAnalysisResult(
                AnalysisResultDto.builder()
                    .id(accepted.getTaskId())
                    .userId(resolvedUserId)
                    .disease(request.getDisease())
                    .inspector(request.getInspector())
                    .job(request.getJob())
                    .baseScore(0)
                    .createdAt(LocalDateTime.now())
                    .deleted(1)
                    .build()
            );

            return accepted;

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        }
    }

    // Input:  taskId (UUID) 작업아이디16자리 랜덤 문자열
    // Output: AnalysisResponseDto (status + data) data-> chatcontet,checklist...
    // 책임:   FastAPI Redis에서 결과 폴링 → COMPLETED 첫 수신 시 ai_db에 결과 저장 (중복 저장 방지)
    @Override
    @Transactional
    public AnalysisResponseDto getAnalysisResult(String taskId) {
        try {
            AnalysisResponseDto response = fastApiClient.get()
                .uri("/api/analysis/{taskId}", taskId)
                .retrieve()
                .body(AnalysisResponseDto.class);

            if ("COMPLETED".equals(response.getStatus()) && response.getData() != null
                    && !analysisResultMapper.existsChatByResultId(taskId)) {
                AnalysisDataDto data = response.getData();
                analysisResultMapper.updateBaseScore(taskId, data.getBaseScore());
                analysisResultMapper.insertChat(taskId, data.getChatContent());

                List checklist = data.getChecklist();
                if (checklist != null && !checklist.isEmpty())
                    analysisResultMapper.insertChecklist(taskId, checklist);

                List warnings = data.getWarning();
                if (warnings != null && !warnings.isEmpty())
                    analysisResultMapper.insertWarnings(taskId, warnings);

                List metaContents = data.getMetaContent();
                if (metaContents != null && !metaContents.isEmpty())
                    analysisResultMapper.insertMetaContents(taskId, metaContents);
            }

            return response;

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        }
    }

    // Input:  ChatRequestDto (context, history, question)
    // Output: ChatResponseDto (answer)
    // 책임:   브라우저 캐시 컨텍스트 + 히스토리를 FastAPI에 그대로 중계 — DB 저장 없음 (stateless)
    @Override
    public ChatResponseDto chatWithAdvisor(ChatRequestDto request) {
        try {
            return fastApiClient.post()
                .uri("/api/analysis/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponseDto.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        }
    }

    // Input:  userId (로그인 유저 ID)
    // Output: List<AnalysisHistoryItemDto> — 빈 리스트 (비로그인) 또는 최신순 이력
    // 책임:   userId 유효성 확인 후 ai_db 이력 조회 위임
    @Override
    public List<AnalysisHistoryItemDto> getMyHistory(String userId) {
        if (userId == null || userId.isBlank()) return Collections.emptyList();
        return analysisResultMapper.findHistoryByUserId(userId);
    }

    // Input:  taskId (analysis_result PK), userId (소유권 검증용)
    // Output: void — 성공 시 정상 종료, 실패 시 404 예외
    // 책임:   논리 삭제 (deleted=0) — userId 불일치 시 404
    @Override
    @Transactional
    public void softDeleteAnalysis(String taskId, String userId) {
        int affected = analysisResultMapper.softDeleteById(taskId, userId);
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "분석 결과를 찾을 수 없거나 권한이 없습니다.");
        }
    }

    // Input:  FastAPI 에러 응답 body (JSON 문자열)
    // Output: detail 메시지 문자열
    // 책임:   FastAPI {"detail": "..."} 형식에서 메시지 추출
    private String extractDetail(String body) {
        try {
            return objectMapper.readTree(body).path("detail").asText("요청 처리 실패");
        } catch (Exception ignored) {
            return "요청 처리 실패";
        }
    }
}
