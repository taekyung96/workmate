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
    @DisplayName("검출된 카드가 한 종류면 자동으로 AUTO 매핑된다")
    void matchCard_singleCard_returnsAuto() {
        var result = service.matchCard(List.of("롯데법인카드"));
        assertThat(result.getSelectType()).isEqualTo("AUTO");
        assertThat(result.getCardName()).isEqualTo("롯데법인카드");
    }

    @Test
    @DisplayName("같은 카드가 여러 항목에 중복 검출돼도 한 종류로 보고 AUTO 매핑된다")
    void matchCard_sameCardDuplicated_returnsAuto() {
        var result = service.matchCard(List.of("롯데법인카드", "롯데법인카드"));
        assertThat(result.getSelectType()).isEqualTo("AUTO");
        assertThat(result.getCardName()).isEqualTo("롯데법인카드");
    }

    @Test
    @DisplayName("서로 다른 카드가 2종 이상 검출되면 MANUAL 매핑된다(사용자가 드롭다운에서 선택)")
    void matchCard_multipleDistinctCards_returnsManual() {
        var result = service.matchCard(List.of("국민카드", "롯데법인카드", "신한카드"));
        assertThat(result.getSelectType()).isEqualTo("MANUAL");
        assertThat(result.getCardName()).isNull();
    }

    @Test
    @DisplayName("검출된 카드가 없으면(빈 목록·공백만) MANUAL 매핑된다")
    void matchCard_noCard_returnsManual() {
        assertThat(service.matchCard(List.of()).getSelectType()).isEqualTo("MANUAL");
        assertThat(service.matchCard(List.of("", "  ")).getSelectType()).isEqualTo("MANUAL");
    }
}
