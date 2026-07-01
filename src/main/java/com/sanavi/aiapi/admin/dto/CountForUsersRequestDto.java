package com.sanavi.aiapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// backend-springboot가 main_db(구독여부·유저타입·직업)로 먼저 추려낸 userIds를 넘겨,
// 이 유저들의 기간 내 분석횟수 총합만 요청 — ai-api는 ai_db 밖의 필터 조건을 알 필요가 없음
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountForUsersRequestDto {
    private String range;
    private List<String> userIds;
}
