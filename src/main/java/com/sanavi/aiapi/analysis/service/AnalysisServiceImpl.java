package com.sanavi.aiapi.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanavi.aiapi.analysis.dto.AnalysisAcceptedDto;
import com.sanavi.aiapi.analysis.dto.AnalysisDataDto;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResultDto;
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

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final RestClient fastApiClient;
    private final ObjectMapper objectMapper;
    private final AnalysisResultMapper analysisResultMapper;

    @Override
    @Transactional
    public AnalysisAcceptedDto requestAnalysis(AnalysisRequestDto request) {
        try {
            AnalysisAcceptedDto accepted = fastApiClient.post()
                .uri("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AnalysisAcceptedDto.class);

            analysisResultMapper.insertAnalysisResult(
                AnalysisResultDto.builder()
                    .id(accepted.getTaskId())
                    .userId(null) // auth 구현 후 추가 default null
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

    private String extractDetail(String body) {
        try {
            return objectMapper.readTree(body).path("detail").asText("요청 처리 실패");
        } catch (Exception ignored) {
            return "요청 처리 실패";
        }
    }
}
