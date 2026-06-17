package com.sanavi.aiapi.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestClient fastApiClient;
    private final ObjectMapper objectMapper;

    public AnalysisResponseDto analyze(AnalysisRequestDto request) {
        try {
            return fastApiClient.post()
                .uri("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AnalysisResponseDto.class);

        } catch (HttpClientErrorException e) {
            String message = extractDetail(e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), message);

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        }
    }

    private String extractDetail(String body) {
        try {
            return objectMapper.readTree(body).path("detail").asText("분석 요청 실패");
        } catch (Exception ignored) {
            return "분석 요청 실패";
        }
    }
}
