package com.workmate.was.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 사용량 집계 조회 기간 (관리자 사용량 대시보드).
 * 요청에 from·to 가 없으면 서버가 최근 30일로 채운 뒤, 실제 적용된 기간을 응답에 그대로 담아준다
 * (날짜 계산의 단일 출처는 서버 — 프론트가 날짜를 다시 계산하지 않는다).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UsagePeriodVo {
    private LocalDate from;
    private LocalDate to;
}
