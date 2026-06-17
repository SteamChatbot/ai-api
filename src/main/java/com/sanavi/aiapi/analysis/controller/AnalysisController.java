package com.sanavi.aiapi.analysis.controller;

import com.sanavi.aiapi.analysis.dto.AnalysisAcceptedDto;
import com.sanavi.aiapi.analysis.dto.AnalysisRequestDto;
import com.sanavi.aiapi.analysis.dto.AnalysisResponseDto;
import com.sanavi.aiapi.analysis.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<AnalysisAcceptedDto> requestAnalysis(@RequestBody @Valid AnalysisRequestDto request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(analysisService.requestAnalysis(request));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<AnalysisResponseDto> getAnalysisResult(@PathVariable String taskId) {
        return ResponseEntity.ok(analysisService.getAnalysisResult(taskId));
    }
}
