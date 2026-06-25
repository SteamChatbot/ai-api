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
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Slf4j
@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final RestClient fastApiRequestClient;
    private final RestClient fastApiPollClient;
    private final ObjectMapper objectMapper;
    private final AnalysisResultMapper analysisResultMapper;

    public AnalysisServiceImpl(
            @Qualifier("fastApiRequestClient") RestClient fastApiRequestClient,
            @Qualifier("fastApiPollClient") RestClient fastApiPollClient,
            ObjectMapper objectMapper,
            AnalysisResultMapper analysisResultMapper) {
        this.fastApiRequestClient = fastApiRequestClient;
        this.fastApiPollClient = fastApiPollClient;
        this.objectMapper = objectMapper;
        this.analysisResultMapper = analysisResultMapper;
    }

    // true(기본값): 비동기 — taskId 즉시 반환, 스레드 바로 반환
    // false: 동기 — FastAPI 완료까지 스레드 블로킹 → Thread Starvation 재현용
    // nGrinder로 false 상태에서 부하 테스트 시 동시 40명에서 TPS 0 수렴 확인 가능
    @Value("${analysis.async:true}")
    private boolean asyncMode;

    // Input:  AnalysisRequestDto (유저 입력값) + userId (X-User-Id 헤더, 비로그인 시 null)
    // Output: AnalysisAcceptedDto (task_id, status="PROCESSING" or "COMPLETED")
    // 책임:   asyncMode에 따라 비동기(즉시 반환) 또는 동기(완료까지 블로킹) 분기
    @Override
    public AnalysisAcceptedDto requestAnalysis(AnalysisRequestDto request, String userId) {
        String resolvedUserId = (userId != null && !userId.isBlank()) ? userId : null;
        if (asyncMode) {
            AnalysisAcceptedDto accepted = callFastApiRequest(request);
            saveInitialAnalysisResult(accepted.getTaskId(), resolvedUserId, request);
            return accepted;
        }
        return requestSync(request, resolvedUserId);
    }

    // 동기 처리 — FastAPI 완료될 때까지 스레드를 블로킹
    // asyncMode=false 일 때만 호출 — 운영 환경에서는 사용 금지
    // Thread Starvation 재현: Tomcat 기본 스레드 200개 / 80초 = 초당 2.5req 처리 한계
    //   → 동시 40명 요청 시 스레드 풀 고갈, TPS 0 수렴
    private AnalysisAcceptedDto requestSync(AnalysisRequestDto request, String userId) {
        AnalysisAcceptedDto accepted = callFastApiRequest(request);
        saveInitialAnalysisResult(accepted.getTaskId(), userId, request);

        // FastAPI 완료될 때까지 2초 간격으로 폴링 — 이 루프가 스레드를 점유
        int maxAttempts = 60; // 최대 120초 대기
        for (int i = 0; i < maxAttempts; i++) {
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            AnalysisResponseDto response = pollFastApi(accepted.getTaskId());
            if ("COMPLETED".equals(response.getStatus()) && response.getData() != null) {
                saveCompletedResult(accepted.getTaskId(), response.getData());
                log.info("[{}] 동기 분석 완료 — taskId={} attempt={}", MDC.get("traceId"), accepted.getTaskId(), i + 1);
                return accepted;
            }
        }
        log.error("[{}] 동기 분석 타임아웃 — taskId={}", MDC.get("traceId"), accepted.getTaskId());
        throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 분석 시간이 초과되었습니다.");
    }

    private AnalysisAcceptedDto callFastApiRequest(AnalysisRequestDto request) {
        long start = System.currentTimeMillis();
        try {
            // Circuit Breaker가 OPEN이면 rawCallFastApiRequest()를 호출하지 않고 즉시 CallNotPermittedException 던짐
            AnalysisAcceptedDto result = rawCallFastApiRequest(request);
            log.info("[{}] FastAPI 분석 요청 완료 — taskId={} {}ms",
                    MDC.get("traceId"), result.getTaskId(), System.currentTimeMillis() - start);
            return result;
        } catch (HttpClientErrorException e) {
            // 4xx — 클라이언트 입력 오류, FastAPI 장애 아님 → Circuit Breaker 실패 카운트에 포함 안 됨
            log.warn("[{}] FastAPI 분석 요청 실패 — status={} {}ms",
                    MDC.get("traceId"), e.getStatusCode(), System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (HttpServerErrorException e) {
            // 5xx — FastAPI 서버 내부 오류 → Circuit Breaker 실패로 기록
            log.error("[{}] FastAPI 서버 오류 — {}ms", MDC.get("traceId"), System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 오류: " + extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            // 타임아웃·네트워크 단절 → Circuit Breaker 실패로 기록
            log.error("[{}] FastAPI 연결 불가 (타임아웃 or 네트워크) — {}ms",
                    MDC.get("traceId"), System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        } catch (CallNotPermittedException e) {
            // Circuit Breaker OPEN 상태 — FastAPI가 불안정하므로 호출 자체를 차단
            // 30초 후 HALF_OPEN으로 전환되어 소수 요청으로 회복 여부 판단
            log.error("[{}] [Circuit Breaker OPEN] FastAPI 호출 차단 — 장애 전파 방지 — {}ms",
                    MDC.get("traceId"), System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버가 일시적으로 이용 불가합니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // Circuit Breaker 적용 지점 — 예외가 래핑되기 전 원본 예외를 정확히 분류
    // recordExceptions: HttpServerErrorException, ResourceAccessException (FastAPI 장애)
    // ignoreExceptions: HttpClientErrorException (클라이언트 책임 → 장애 카운트 제외)
    @CircuitBreaker(name = "fastapi")
    private AnalysisAcceptedDto rawCallFastApiRequest(AnalysisRequestDto request) {
        return fastApiRequestClient.post()
            .uri("/api/analysis")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(AnalysisAcceptedDto.class);
    }

    // 격리수준: REPEATABLE_READ (MariaDB 기본값)
    // HTTP 호출 완료 후 짧게 트랜잭션 열어 INSERT만 처리 — 커넥션 점유 시간 최소화
    @Transactional
    public void saveInitialAnalysisResult(String taskId, String userId, AnalysisRequestDto request) {
        analysisResultMapper.insertAnalysisResult(
            AnalysisResultDto.builder()
                .id(taskId)
                .userId(userId)
                .disease(request.getDisease())
                .inspector(request.getInspector())
                .job(request.getJob())
                .baseScore(0)
                .createdAt(LocalDateTime.now())
                .deleted(1)
                .build()
        );
    }

    // Input:  taskId (UUID) 작업아이디16자리 랜덤 문자열
    // Output: AnalysisResponseDto (status + data) data-> chatcontet,checklist...
    // 책임:   DB에 완료 결과 있으면 바로 반환 / 없으면 FastAPI 폴링(트랜잭션 밖) → COMPLETED 시 DB 저장(트랜잭션 안)
    @Override
    public AnalysisResponseDto getAnalysisResult(String taskId) {
        if (isResultStoredInDb(taskId)) {
            return loadFromDb(taskId);
        }

        AnalysisResponseDto response = pollFastApi(taskId);

        if ("COMPLETED".equals(response.getStatus()) && response.getData() != null) {
            saveCompletedResult(taskId, response.getData());
        }

        return response;
    }

    // 격리수준: REPEATABLE_READ / readOnly — 단순 존재 여부 확인용, 쓰기 락 불필요
    @Transactional(readOnly = true)
    public boolean isResultStoredInDb(String taskId) {
        return analysisResultMapper.existsChatByResultId(taskId);
    }

    private AnalysisResponseDto pollFastApi(String taskId) {
        long start = System.currentTimeMillis();
        try {
            AnalysisResponseDto response = rawPollFastApi(taskId);
            // 폴링 결과 상태 로그 — PROCESSING이 얼마나 오래 지속되는지 추적 가능
            log.info("[{}] FastAPI 폴링 결과 — taskId={} status={} {}ms",
                    MDC.get("traceId"), taskId, response.getStatus(), System.currentTimeMillis() - start);
            return response;
        } catch (HttpClientErrorException e) {
            log.warn("[{}] FastAPI 폴링 실패 — taskId={} status={} {}ms",
                    MDC.get("traceId"), taskId, e.getStatusCode(), System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (HttpServerErrorException e) {
            log.error("[{}] FastAPI 서버 오류 — taskId={} {}ms",
                    MDC.get("traceId"), taskId, System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 오류: " + extractDetail(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            log.error("[{}] FastAPI 연결 불가 — taskId={} {}ms",
                    MDC.get("traceId"), taskId, System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        } catch (CallNotPermittedException e) {
            log.error("[{}] [Circuit Breaker OPEN] FastAPI 폴링 차단 — taskId={} {}ms",
                    MDC.get("traceId"), taskId, System.currentTimeMillis() - start);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버가 일시적으로 이용 불가합니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @CircuitBreaker(name = "fastapi")
    private AnalysisResponseDto rawPollFastApi(String taskId) {
        return fastApiPollClient.get()
            .uri("/api/analysis/{taskId}", taskId)
            .retrieve()
            .body(AnalysisResponseDto.class);
    }

    @Transactional
    public void saveCompletedResult(String taskId, AnalysisDataDto data) {
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

    // Input:  ChatRequestDto (context, history, question)
    // Output: ChatResponseDto (answer)
    // 책임:   브라우저 캐시 컨텍스트 + 히스토리를 FastAPI에 그대로 중계 — DB 저장 없음 (stateless)
    @Override
    public ChatResponseDto chatWithAdvisor(ChatRequestDto request) {
        try {
            return fastApiRequestClient.post()
                .uri("/api/analysis/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponseDto.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), extractDetail(e.getResponseBodyAsString()));
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 오류: " + extractDetail(e.getResponseBodyAsString()));
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

    // Input:  taskId
    // Output: AnalysisResponseDto (COMPLETED) — Redis 만료 후 ai_db에서 복원
    // 책임:   FastAPI 404 시 폴백으로 ai_db 직접 조회
    private AnalysisResponseDto loadFromDb(String taskId) {
        AnalysisDataDto base = analysisResultMapper.findBaseByTaskId(taskId);
        if (base == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 작업입니다.");
        }
        base.setChecklist(analysisResultMapper.findChecklistByResultId(taskId));
        base.setWarning(analysisResultMapper.findWarningsByResultId(taskId));
        base.setMetaContent(analysisResultMapper.findMetaContentsByResultId(taskId));
        return AnalysisResponseDto.builder()
                .success(true)
                .status("COMPLETED")
                .message("분석이 완료되었습니다.")
                .data(base)
                .build();
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
