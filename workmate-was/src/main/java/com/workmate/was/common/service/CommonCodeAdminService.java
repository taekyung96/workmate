package com.workmate.was.common.service;

import com.workmate.was.common.dao.CommonCodeGroupRepository;
import com.workmate.was.common.dao.CommonCodeRepository;
import com.workmate.was.common.vo.CommonCode;
import com.workmate.was.common.vo.CommonCodeAdminVo;
import com.workmate.was.common.vo.CommonCodeGroup;
import com.workmate.was.common.vo.CommonCodeGroupSaveVo;
import com.workmate.was.common.vo.CommonCodeGroupVo;
import com.workmate.was.common.vo.CommonCodeId;
import com.workmate.was.common.vo.CommonCodeSaveVo;
import com.workmate.was.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공통코드 관리 서비스 (관리자 전용 CRUD). 그룹·코드의 등록/수정/삭제를 담당한다.
 * 조회 전용 {@link CommonCodeService}(K1)와 분리해 읽기/쓰기 책임을 나눈다.
 * 입력 오류는 IllegalArgumentException(400), 규칙 위반(중복·참조)은 BusinessException(409)로 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonCodeAdminService {

    private final CommonCodeGroupRepository groupRepository;
    private final CommonCodeRepository codeRepository;

    // ----- 그룹 -----

    /** 그룹 전체 목록 (비활성 포함) */
    @Transactional(readOnly = true)
    public List<CommonCodeGroupVo> getGroups() {
        return groupRepository.findAllByOrderByGroupCode().stream()
                .map(CommonCodeGroupVo::from)
                .toList();
    }

    /** 그룹 등록 */
    @Transactional
    public CommonCodeGroupVo createGroup(CommonCodeGroupSaveVo request) {
        String groupCode = require(request.groupCode(), "그룹 코드");
        String groupName = require(request.groupName(), "그룹명");
        if (groupRepository.existsById(groupCode)) {
            throw new BusinessException("이미 존재하는 그룹 코드입니다: " + groupCode);
        }
        CommonCodeGroup saved = groupRepository.save(CommonCodeGroup.builder()
                .groupCode(groupCode)
                .groupName(groupName)
                .description(request.description())
                .useYn(request.useYnOrDefault())
                .build());
        log.info("공통코드 그룹 등록 - group: {}", groupCode);
        return CommonCodeGroupVo.from(saved);
    }

    /** 그룹 수정 (group_code 는 불변) */
    @Transactional
    public CommonCodeGroupVo updateGroup(String groupCode, CommonCodeGroupSaveVo request) {
        CommonCodeGroup group = findGroup(groupCode);
        group.update(require(request.groupName(), "그룹명"), request.description(), request.useYnOrDefault());
        return CommonCodeGroupVo.from(group);
    }

    /** 그룹 삭제 — 하위 코드가 있으면 삭제 불가 (F7) */
    @Transactional
    public void deleteGroup(String groupCode) {
        findGroup(groupCode);
        if (codeRepository.existsByGroupCode(groupCode)) {
            throw new BusinessException("하위 코드가 있는 그룹은 삭제할 수 없습니다. 코드를 먼저 삭제하세요.");
        }
        groupRepository.deleteById(groupCode);
        log.info("공통코드 그룹 삭제 - group: {}", groupCode);
    }

    // ----- 코드 -----

    /** 그룹 내 코드 목록 (비활성 포함) */
    @Transactional(readOnly = true)
    public List<CommonCodeAdminVo> getCodes(String groupCode) {
        findGroup(groupCode);
        return codeRepository.findByGroupCodeOrderBySortOrder(groupCode).stream()
                .map(CommonCodeAdminVo::from)
                .toList();
    }

    /** 코드 등록 */
    @Transactional
    public CommonCodeAdminVo createCode(String groupCode, CommonCodeSaveVo request) {
        findGroup(groupCode);
        String code = require(request.code(), "코드");
        String codeName = require(request.codeName(), "코드명");
        if (codeRepository.existsById(new CommonCodeId(groupCode, code))) {
            throw new BusinessException("이미 존재하는 코드입니다: " + code);
        }
        CommonCode saved = codeRepository.save(CommonCode.builder()
                .groupCode(groupCode)
                .code(code)
                .codeName(codeName)
                .sortOrder(request.sortOrderOrDefault())
                .useYn(request.useYnOrDefault())
                .build());
        log.info("공통코드 등록 - group: {}, code: {}", groupCode, code);
        return CommonCodeAdminVo.from(saved);
    }

    /** 코드 수정 (code 는 불변 — 이름·정렬·사용여부만) */
    @Transactional
    public CommonCodeAdminVo updateCode(String groupCode, String code, CommonCodeSaveVo request) {
        CommonCode found = codeRepository.findById(new CommonCodeId(groupCode, code))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코드입니다: " + code));
        found.update(require(request.codeName(), "코드명"), request.sortOrderOrDefault(), request.useYnOrDefault());
        return CommonCodeAdminVo.from(found);
    }

    /** 코드 삭제 */
    @Transactional
    public void deleteCode(String groupCode, String code) {
        CommonCodeId id = new CommonCodeId(groupCode, code);
        if (!codeRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 코드입니다: " + code);
        }
        codeRepository.deleteById(id);
        log.info("공통코드 삭제 - group: {}, code: {}", groupCode, code);
    }

    private CommonCodeGroup findGroup(String groupCode) {
        return groupRepository.findById(groupCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다: " + groupCode));
    }

    /** 필수 값 검증 — 비어 있으면 400(IllegalArgumentException), 통과하면 trim 값 반환 */
    private String require(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
