package com.workmate.was.receipt.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptServiceImplTest {

    // 순수 로직 테스트라 OcrService·ReceiptRepository 의존성은 null로 충분 (호출되지 않음).
    // 저장 루트는 파일 IO 를 타지 않는 테스트라 임의 경로 문자열이면 된다.
    private final ReceiptServiceImpl service = new ReceiptServiceImpl(null, null, "uploads/receipt");

    @Test
    @DisplayName("유효한 한국 사업자등록번호(삼성전자)는 체크섬 검증을 통과한다")
    void validateBizNo_valid_returnsTrue() {
        assertThat(service.isValidBizNo("2208162517")).isTrue();
    }

    @Test
    @DisplayName("유효하지 않은 사업자등록번호는 체크섬 검증에 실패한다")
    void validateBizNo_invalid_returnsFalse() {
        assertThat(service.isValidBizNo("1234567890")).isFalse();
    }

    @Test
    @DisplayName("자리수가 10자리가 아니거나 숫자가 아닌 입력은 검증에 실패한다")
    void validateBizNo_wrongFormat_returnsFalse() {
        assertThat(service.isValidBizNo("123")).isFalse();
        assertThat(service.isValidBizNo("abcdefghij")).isFalse();
        assertThat(service.isValidBizNo("")).isFalse();
        assertThat(service.isValidBizNo(null)).isFalse();
    }

    @Test
    @DisplayName("롯데법인카드인 내역이 단 1건 존재하면 자동으로 AUTO 매핑된다")
    void matchCard_oneLotteCorporateCard_returnsAuto() {
        var result = service.matchCard(List.of("국민카드", "롯데법인카드", "신한카드"));
        assertThat(result.getSelectType()).isEqualTo("AUTO");
        assertThat(result.getCardName()).isEqualTo("롯데법인카드");
    }

    @Test
    @DisplayName("롯데법인카드인 내역이 존재하지 않으면 MANUAL 매핑된다")
    void matchCard_noLotteCorporateCard_returnsManual() {
        var result = service.matchCard(List.of("국민카드", "현대카드", "신한카드"));
        assertThat(result.getSelectType()).isEqualTo("MANUAL");
        assertThat(result.getCardName()).isNull();
    }

    @Test
    @DisplayName("롯데법인카드인 내역이 2건 이상 존재하면 MANUAL 매핑된다")
    void matchCard_multipleLotteCorporateCards_returnsManual() {
        var result = service.matchCard(List.of("롯데법인카드", "현대카드", "롯데법인카드"));
        assertThat(result.getSelectType()).isEqualTo("MANUAL");
        assertThat(result.getCardName()).isNull();
    }
}
