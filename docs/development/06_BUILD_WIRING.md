# 06. 빌드 연결 — Vue(SPA) 산출물을 WEB에 붙이기

- **관련**: HANDOVER 4단계(빌드 연결), [01_ARCHITECTURE](01_ARCHITECTURE.md), [05_WEB_BFF_RECONSTRUCTION](05_WEB_BFF_RECONSTRUCTION.md)

> 이 문서는 "Vue3 SPA를 빌드해서 WEB(8080)에 얹어 **8080 하나로** 서비스하는" 배선을 설명한다.

---

## 1. 왜 필요한가 — 개발 모드 vs 운영 모드

### 개발(dev) 모드 — 서버 4개

```
브라우저 ─▶ Vite(5173) ─(/api 프록시)▶ WEB(8080) ─▶ WAS(8081) ─▶ DB(5432)
            └ Vue를 핫리로드로 서빙(개발 편의)
```

- Vite가 Vue 소스를 즉시 반영(HMR)해줘 **프론트 개발이 빠르다**.
- `/api` 요청은 `vite.config.ts` 의 `server.proxy` 가 8080으로 넘긴다.

### 운영(prod) 모드 — 서버 3개

```
브라우저 ─▶ WEB(8080) ─▶ WAS(8081) ─▶ DB
            └ Vue 빌드 산출물(dist)을 직접 서빙 (Vite 없음)
```

- **브라우저는 8080만 바라본다** (설계 원칙). Vite는 배포에 없다.
- Vue를 **빌드**하면 `dist/`(정적파일: `index.html` + `assets/*.js` + `*.css`)가 나오고, 이걸 WEB이 웹서버처럼 서빙한다.

**핵심**: "Vue를 빌드해서 WEB에 붙인다" = **dist 폴더를 WEB의 정적 리소스 경로(`classpath:/static/`)에 넣는다**.

---

## 2. SPA 서빙이 동작하는 원리 (`SpaWebConfig`)

`workmate-web/.../global/web/SpaWebConfig.java` 가 정적 서빙 + 딥링크 fallback을 담당한다.

- 요청 경로에 **실제 파일이 있으면** 그대로 서빙 (`index.html`, `assets/app.js` 등)
- 파일이 **없는 경로**(예: `/chat`, `/guide/3` 를 브라우저에서 새로고침)는 → **`index.html` 로 fallback** → 로딩된 Vue Router가 화면을 그림
- `/api/**` 는 REST 컨트롤러가 우선 처리 (정적 서빙 대상에서 제외)

> ⚠️ 과거의 `SpaForwardController`(`/{*path}` catch-all)는 `.js`·`.css` 정적 파일까지 삼켜서 폐기했다. 정적 파일은 반드시 리소스 핸들러가 먼저 서빙해야 한다.

---

## 3. dist를 붙이는 두 가지 방법

### 방법 A — Gradle 빌드 연동 (현재 채택, 원클릭)

`workmate-web/build.gradle` 에 다음을 두었다:

```gradle
def vueDir = file("${rootDir}/workmate-vue")

tasks.register('buildVue', Exec) {          // ① Vue를 프로덕션 빌드
    workingDir vueDir
    commandLine 'cmd', '/c', 'npm', 'run', 'build'   // (Windows 기준)
}

processResources {
    dependsOn 'buildVue'                     // ② WEB 리소스 처리 전에 Vue 빌드
    from("${vueDir}/dist") { into 'static' } // ③ dist를 classpath:/static/ 으로 복사
}
```

- **동작**: `gradlew :workmate-web:bootRun`(또는 `bootJar`) 한 번 → 자동으로 Vue 빌드 → dist를 static에 복사 → 8080이 SPA까지 서빙.
- **장점**: 원클릭. 배포 산출물(jar) 하나에 프론트까지 포함 → CI/배포가 단순.
- **단점**: WEB을 gradle로 띄울 때마다 npm 빌드가 돌아 **느리다**. 그래서 **프론트만 개발할 땐 이 경로 대신 Vite(5173)** 를 쓴다.
- **전제**: 빌드 머신에 Node/npm 설치 + PATH 등록.

**명령**

```bash
# 운영 방식(8080만)으로 통합 실행 — Vue 빌드까지 자동
./gradlew :workmate-web:bootRun
# 배포용 jar (프론트 포함)
./gradlew :workmate-web:bootJar
```

### 방법 B — 수동 복사 (간단, 학습용)

```bash
# 1) Vue 빌드
cd workmate-vue && npm run build          # dist/ 생성

# 2) dist 내용을 WEB 정적 경로에 복사
#    (src/main/resources/static 은 git에 안 올라가게 .gitignore 처리 권장)
cp -r dist/* ../workmate-web/src/main/resources/static/

# 3) WEB 실행
cd .. && ./gradlew :workmate-web:bootRun
```

- **장점**: 이해하기 쉽고 gradle 설정이 필요 없다.
- **단점**: 프론트 고칠 때마다 **수동으로 빌드+복사** 반복. 실수하기 쉽고 자동화가 안 됨.
- **주의**: `src/main/resources/static/` 에 dist를 직접 넣으면 **소스 저장소가 오염**되므로, 넣더라도 `.gitignore` 로 제외한다. (방법 A는 `build/` 출력에만 복사되어 이 문제가 없다.)

---

## 4. 언제 무엇을 쓰나 (권장 워크플로우)

| 상황                             | 실행                                         | 서버             |
| -------------------------------- | -------------------------------------------- | ---------------- |
| **프론트 개발** (화면 자주 수정) | `npm run dev` (5173) + WEB/WAS 따로          | Vite·WEB·WAS·DB  |
| **통합 확인 / 운영과 동일 검증** | `./gradlew :workmate-web:bootRun` (방법 A)   | WEB(8080)·WAS·DB |
| **배포**                         | `./gradlew :workmate-web:bootJar` → jar 실행 | WEB·WAS·DB       |

> 정리: **개발은 Vite(5173)로 빠르게, 통합·배포는 방법 A로 8080 하나만.**
