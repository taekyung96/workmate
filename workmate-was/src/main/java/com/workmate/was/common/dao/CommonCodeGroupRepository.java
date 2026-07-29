package com.workmate.was.common.dao;

import com.workmate.was.common.vo.CommonCodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 공통코드 그룹 Repository (관리자 CRUD, F7).
 */
public interface CommonCodeGroupRepository extends JpaRepository<CommonCodeGroup, String> {

    /** 그룹 전체 목록 (코드순) */
    List<CommonCodeGroup> findAllByOrderByGroupCode();
}
