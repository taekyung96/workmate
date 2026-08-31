# =============================================================
# workmate-web 컨테이너 이미지 (멀티스테이지 빌드)
# ① Node 로 SPA 빌드 → ② Gradle 로 부트 JAR → ③ JRE 로 실행
#
# :workmate-web:processResources 가 buildVue(npm run build)를 호출하므로 원래는 Gradle
# 스테이지에 Node 가 필요하다. 여기서는 SPA 를 전용 스테이지에서 먼저 만들어 dist 만 넘기고
# -PskipVueBuild 로 중복 빌드를 건너뛴다 — 실행 이미지에 Node 를 남기지 않기 위함이다.
# 빌드 컨텍스트는 저장소 루트 (모노레포 settings.gradle 이 web/was 를 함께 include).
# =============================================================

# ---- 1) SPA 빌드 스테이지 ----
FROM node:24-alpine AS spa
WORKDIR /spa
# 의존성 정의를 먼저 복사 → 소스만 바뀌면 npm ci 레이어를 캐시에서 재사용한다
COPY workmate-vue/package.json workmate-vue/package-lock.json ./
RUN npm ci
COPY workmate-vue/ ./
# type-check + vite build (package.json 의 build 스크립트)
RUN npm run build

# ---- 2) Gradle 빌드 스테이지: bootJar 생성 ----
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
# ①의 산출물을 processResources 가 집어가는 위치(workmate-vue/dist)에 놓는다
COPY --from=spa /spa/dist workmate-vue/dist
# 테스트는 실제 DB(pgvector) 가 필요하므로 이미지 빌드 시에는 제외한다 (기동은 compose 로 검증)
RUN chmod +x gradlew && ./gradlew :workmate-web:bootJar -x test --no-daemon -PskipVueBuild

# ---- 3) 실행 스테이지: JRE 로 슬림하게 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# *-SNAPSHOT.jar 패턴은 부트 JAR 만 매칭한다 ( -plain.jar 는 제외됨 )
COPY --from=builder /app/workmate-web/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
