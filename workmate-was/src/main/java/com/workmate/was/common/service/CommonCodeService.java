package com.workmate.was.common.service;

import com.workmate.was.common.dao.CommonCodeRepository;
import com.workmate.was.common.vo.CommonCode;
import com.workmate.was.common.vo.CommonCodeId;
import com.workmate.was.common.vo.CommonCodeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 공통코드 서비스 (F7). 그룹별 코드 조회(K1)와 화이트리스트 검증(F9-04)을 제공한다.
 */
@Service
@RequiredArgsConstructor
public class CommonCodeService {

    private final CommonCodeRepository commonCodeRepository;

    /** 그룹 코드 목록 — 존재하지 않는 그룹은 빈 배열 (F7.3) */
    @Transactional(readOnly = true)
    public List<CommonCodeVo> getCodes(String groupCode) {
        return commonCodeRepository.findByGroupCodeAndUseYnTrueOrderBySortOrder(groupCode).stream()
                .map(c -> new CommonCodeVo(c.getCode(), c.getCodeName()))
                .toList();
    }

    /** 해당 그룹의 사용 중 코드인지 (화이트리스트, F9-04) */
    @Transactional(readOnly = true)
    public boolean isValidCode(String groupCode, String code) {
        return commonCodeRepository.existsByGroupCodeAndCodeAndUseYnTrue(groupCode, code);
    }

    /**
     * 코드의 부가 속성(attr1)을 읽는다. 의미는 그룹마다 다르다 —
     * AI_MODEL 에서는 LLM 제공자(google-genai | openai)다.
     *
     * @param groupCode 그룹 코드
     * @param code      코드
     * @return attr1 값. 코드가 없거나 값이 비어 있으면 빈 Optional
     */
    @Transactional(readOnly = true)
    public Optional<String> findAttr1(String groupCode, String code) {
        return commonCodeRepository.findById(new CommonCodeId(groupCode, code))
                .map(CommonCode::getAttr1)
                .filter(v -> v != null && !v.isBlank());
    }
}
