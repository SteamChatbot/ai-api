package com.sanavi.aiapi.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${ai-fastapi.url}")
    private String fastApiUrl;

    // AI 분석 요청용 — FastAPI가 실제 분석을 수행하므로 응답까지 최대 120초 대기
    @Bean("fastApiRequestClient")
    public RestClient fastApiRequestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        return RestClient.builder()
            .baseUrl(fastApiUrl)
            .requestFactory(factory)
            .build();
    }

    // 폴링용 — FastAPI가 Redis에서 상태만 읽으므로 응답이 즉시 와야 정상
    // 10초 이상 걸리면 FastAPI 자체에 문제가 생긴 것 → 빠르게 실패하고 Circuit Breaker에 기록
    @Bean("fastApiPollClient")
    public RestClient fastApiPollClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        return RestClient.builder()
            .baseUrl(fastApiUrl)
            .requestFactory(factory)
            .build();
    }
}
