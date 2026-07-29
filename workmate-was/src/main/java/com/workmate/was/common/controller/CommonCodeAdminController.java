package com.workmate.was.common.controller;

import com.workmate.was.common.service.CommonCodeAdminService;
import com.workmate.was.common.vo.CommonCodeAdminVo;
import com.workmate.was.common.vo.CommonCodeGroupSaveVo;
import com.workmate.was.common.vo.CommonCodeGroupVo;
import com.workmate.was.common.vo.CommonCodeSaveVo;
import com.workmate.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공통코드 관리 REST API (관리자 전용, F7). 그룹·코드 CRUD.
 * 접근 제어(ROLE_ADMIN)는 WEB Security 의 /api/v1/admin/** 규칙이 담당한다.
 */
@RestController
@RequestMapping("/api/v1/admin/common-codes")
@RequiredArgsConstructor
public class CommonCodeAdminController {

    private final CommonCodeAdminService commonCodeAdminService;

    /** 그룹 목록 (비활성 포함) */
    @GetMapping("/groups")
    public ApiResponse<List<CommonCodeGroupVo>> getGroups() {
        return ApiResponse.success(commonCodeAdminService.getGroups());
    }

    /** 그룹 등록 */
    @PostMapping("/groups")
    public ApiResponse<CommonCodeGroupVo> createGroup(@RequestBody CommonCodeGroupSaveVo request) {
        return ApiResponse.success(commonCodeAdminService.createGroup(request));
    }

    /** 그룹 수정 */
    @PutMapping("/groups/{groupCode}")
    public ApiResponse<CommonCodeGroupVo> updateGroup(
            @PathVariable("groupCode") String groupCode,
            @RequestBody CommonCodeGroupSaveVo request) {
        return ApiResponse.success(commonCodeAdminService.updateGroup(groupCode, request));
    }

    /** 그룹 삭제 (하위 코드 없을 때만) */
    @DeleteMapping("/groups/{groupCode}")
    public ApiResponse<Void> deleteGroup(@PathVariable("groupCode") String groupCode) {
        commonCodeAdminService.deleteGroup(groupCode);
        return ApiResponse.success();
    }

    /** 그룹 내 코드 목록 (비활성 포함) */
    @GetMapping("/groups/{groupCode}/codes")
    public ApiResponse<List<CommonCodeAdminVo>> getCodes(@PathVariable("groupCode") String groupCode) {
        return ApiResponse.success(commonCodeAdminService.getCodes(groupCode));
    }

    /** 코드 등록 */
    @PostMapping("/groups/{groupCode}/codes")
    public ApiResponse<CommonCodeAdminVo> createCode(
            @PathVariable("groupCode") String groupCode,
            @RequestBody CommonCodeSaveVo request) {
        return ApiResponse.success(commonCodeAdminService.createCode(groupCode, request));
    }

    /** 코드 수정 */
    @PutMapping("/groups/{groupCode}/codes/{code}")
    public ApiResponse<CommonCodeAdminVo> updateCode(
            @PathVariable("groupCode") String groupCode,
            @PathVariable("code") String code,
            @RequestBody CommonCodeSaveVo request) {
        return ApiResponse.success(commonCodeAdminService.updateCode(groupCode, code, request));
    }

    /** 코드 삭제 */
    @DeleteMapping("/groups/{groupCode}/codes/{code}")
    public ApiResponse<Void> deleteCode(
            @PathVariable("groupCode") String groupCode,
            @PathVariable("code") String code) {
        commonCodeAdminService.deleteCode(groupCode, code);
        return ApiResponse.success();
    }
}
