package com.workmate.was.global.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 설정 클래스.
 * - @Mapper 애노테이션이 명시된 인터페이스만 스캔하여 JPA Repository와의 Bean 충돌을 방지한다.
 * - 도메인별 동적 기간·GROUP BY 집계가 필요할 때 각 도메인 dao/ 아래 ~Mapper 인터페이스 +
 *   resources/mapper/[도메인명]/*.xml로 추가한다(XML 위치는 application.yml 의 mybatis.mapper-locations).
 *   첫 사례는 usage/dao/LlmUsageQueryMapper(관리자 사용량 대시보드).
 */
@Configuration
@MapperScan(basePackages = "com.workmate.was.**.dao", annotationClass = Mapper.class)
public class MyBatisConfig {
}
